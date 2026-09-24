#!/usr/bin/env bash
# Executa os testes E2E (Playwright) contra o sistema completo.
#
# 1. sobe o PostgreSQL do compose.yaml (credenciais do .env da raiz);
# 2. cria um banco temporário exclusivo e vazio (nunca o banco de desenvolvimento);
# 3. empacota o backend (o Flyway cria o schema ao iniciar; nenhum usuário é criado);
# 4. gera segredo JWT e a senha que o teste usa para criar o ADMIN na configuração inicial,
#    aleatórios e válidos só nesta execução;
# 5. roda o Playwright (que inicia backend e frontend) e remove o banco no final.
#
# Uso: npm run e2e [-- argumentos do Playwright]
# Variáveis opcionais: E2E_POSTGRES_DB (padrão valora_e2e), E2E_MANTER_BANCO=1.
set -euo pipefail

FRONTEND="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAIZ="$(cd "$FRONTEND/.." && pwd)"
COMPOSE=(docker compose -f "$RAIZ/compose.yaml")

if [ ! -f "$RAIZ/.env" ]; then
  echo "Arquivo .env não encontrado na raiz. Copie .env.example para .env e ajuste os valores." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
. "$RAIZ/.env"
set +a

export E2E_POSTGRES_DB="${E2E_POSTGRES_DB:-valora_e2e}"
if [ "$E2E_POSTGRES_DB" = "$POSTGRES_DB" ]; then
  echo "E2E_POSTGRES_DB não pode ser o banco de desenvolvimento ($POSTGRES_DB)." >&2
  exit 1
fi
if ! [[ "$E2E_POSTGRES_DB" =~ ^[a-z0-9_]+$ ]]; then
  echo "E2E_POSTGRES_DB deve conter apenas letras minúsculas, números e _." >&2
  exit 1
fi

for porta in 8080 5173; do
  if (exec 3<>"/dev/tcp/127.0.0.1/$porta") 2>/dev/null; then
    echo "A porta $porta está em uso. Pare o backend/frontend em execução antes do E2E." >&2
    exit 1
  fi
done

psql_admin() {
  "${COMPOSE[@]}" exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -qc "$1"
}

echo "==> PostgreSQL"
"${COMPOSE[@]}" up -d postgres
until "${COMPOSE[@]}" exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
  sleep 1
done

remover_banco() {
  if [ "${E2E_MANTER_BANCO:-0}" != "1" ]; then
    psql_admin "DROP DATABASE IF EXISTS $E2E_POSTGRES_DB WITH (FORCE)" || true
  fi
}
trap remover_banco EXIT

psql_admin "DROP DATABASE IF EXISTS $E2E_POSTGRES_DB WITH (FORCE)"
psql_admin "CREATE DATABASE $E2E_POSTGRES_DB"
echo "==> Banco temporário: $E2E_POSTGRES_DB"

echo "==> Empacotando o backend"
(cd "$RAIZ/backend" && mvn -q -DskipTests package)
E2E_JAR="$(ls "$RAIZ"/backend/target/compara-precos-*.jar | head -n 1)"
export E2E_JAR

E2E_JWT_SECRET="$(openssl rand -base64 48)"
E2E_ADMIN_SENHA="$(openssl rand -hex 16)"
export E2E_JWT_SECRET E2E_ADMIN_SENHA
export E2E_ADMIN_EMAIL="admin.e2e@teste.local"

echo "==> Playwright"
cd "$FRONTEND"
npx playwright test "$@"
