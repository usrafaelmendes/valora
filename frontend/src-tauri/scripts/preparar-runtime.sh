#!/usr/bin/env bash
# Monta src-tauri/runtime/<plataforma>/ (ignorada pelo Git) com os componentes que o Valora
# distribuído inicia sozinho (ver src/componentes_locais.rs e src/plataforma.rs):
#
#   java/                       runtime Java 21 reduzido (jlink com os jmods do JDK da plataforma)
#   backend/valora-backend.jar  backend Spring Boot (mvn package)
#   postgresql/                 PostgreSQL 17 portátil da plataforma
#
# Uso (a partir de Linux/WSL):
#   bash scripts/preparar-runtime.sh windows   # -> runtime/windows-x86_64/
#   bash scripts/preparar-runtime.sh linux     # -> runtime/linux-x86_64/
#
# Requisitos da máquina de build: JDK 21 (jlink), Maven, curl, python3 e, para linux, Docker
# (somente para extrair o PostgreSQL da imagem oficial; o produto final não usa Docker).
# Os downloads ficam em cache em ${VALORA_CACHE_DIR:-~/.cache/valora-runtime}, com SHA-256 conferido.
set -euo pipefail

PLATAFORMA="${1:-}"
case "$PLATAFORMA" in
  windows) DESTINO_NOME="windows-x86_64" ;;
  linux) DESTINO_NOME="linux-x86_64" ;;
  *)
    echo "Uso: $0 windows|linux" >&2
    exit 1
    ;;
esac

TAURI="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAIZ="$(cd "$TAURI/../.." && pwd)"
DESTINO="$TAURI/runtime/$DESTINO_NOME"
CACHE="${VALORA_CACHE_DIR:-$HOME/.cache/valora-runtime}"

# ---- Versões fixadas -------------------------------------------------------------------------
# JDK 21 Temurin (Eclipse Adoptium): fornece os jmods da plataforma para o jlink.
JDK_VERSAO="21.0.12.1+1"
JDK_WINDOWS_ARQUIVO="OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.zip"
JDK_WINDOWS_SHA256="f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e"
JDK_LINUX_ARQUIVO="OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz"
JDK_LINUX_SHA256="ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94"
JDK_URL_BASE="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${JDK_VERSAO/+/%2B}"

# PostgreSQL 17 para Windows: pacote "binaries only" oficial da EDB (a EDB não publica o SHA-256;
# o valor abaixo foi registrado no primeiro download e passa a ser conferido).
PG_WINDOWS_ARQUIVO="postgresql-17.11-1-windows-x64-binaries.zip"
PG_WINDOWS_SHA256="6eabdf00d2893713b75db4336a23c3fdf505f056e217ec6e2e95d901750cfea3"
PG_WINDOWS_URL="https://get.enterprisedb.com/postgresql/$PG_WINDOWS_ARQUIVO"

# PostgreSQL 17 para Linux: binários PGDG da imagem Docker oficial baseada no Debian 12
# (glibc 2.36), com as bibliotecas de que dependem, exceto as da glibc.
PG_LINUX_IMAGEM="postgres:17.11-bookworm"

# Módulos Java levantados com jdeps sobre o jar do backend, mais jdk.zipfs, jdk.charsets e
# jdk.localedata (formatação pt-BR) e java.naming (JNDI usado pelo Tomcat/Hibernate).
MODULOS_JAVA="java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,\
java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,java.sql.rowset,\
jdk.charsets,jdk.jfr,jdk.localedata,jdk.unsupported,jdk.zipfs"

# ---- Funções ---------------------------------------------------------------------------------
baixar() { # arquivo url sha256
  local arquivo="$CACHE/$1"
  mkdir -p "$CACHE"
  if [ ! -f "$arquivo" ]; then
    echo "    baixando $1"
    curl -fsSL --retry 3 -o "$arquivo.parcial" "$2"
    mv "$arquivo.parcial" "$arquivo"
  fi
  if [ -n "$3" ]; then
    echo "$3  $arquivo" | sha256sum -c --quiet - || {
      echo "SHA-256 não confere: $arquivo" >&2
      exit 1
    }
  fi
}

extrair_zip() { # zip destino [prefixos...]
  python3 - "$@" <<'PY'
import sys, zipfile
zip_, destino, *prefixos = sys.argv[1:]
with zipfile.ZipFile(zip_) as z:
    membros = [m for m in z.namelist() if not prefixos or m.startswith(tuple(prefixos))]
    z.extractall(destino, membros)
PY
}

# ---- Backend ---------------------------------------------------------------------------------
echo "==> Backend (mvn package)"
(cd "$RAIZ/backend" && mvn -q -DskipTests package)
JAR="$(ls "$RAIZ"/backend/target/compara-precos-*.jar | head -n 1)"

rm -rf "$DESTINO"
mkdir -p "$DESTINO/backend"
cp "$JAR" "$DESTINO/backend/valora-backend.jar"

TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT

# ---- Java ------------------------------------------------------------------------------------
echo "==> Runtime Java $JDK_VERSAO ($PLATAFORMA, jlink)"
if [ "$PLATAFORMA" = windows ]; then
  baixar "$JDK_WINDOWS_ARQUIVO" "$JDK_URL_BASE/$JDK_WINDOWS_ARQUIVO" "$JDK_WINDOWS_SHA256"
  extrair_zip "$CACHE/$JDK_WINDOWS_ARQUIVO" "$TEMP/jdk"
else
  baixar "$JDK_LINUX_ARQUIVO" "$JDK_URL_BASE/$JDK_LINUX_ARQUIVO" "$JDK_LINUX_SHA256"
  mkdir -p "$TEMP/jdk" && tar -xzf "$CACHE/$JDK_LINUX_ARQUIVO" -C "$TEMP/jdk"
fi
JMODS="$(echo "$TEMP"/jdk/*/jmods)"
# O jlink local (JDK 21) gera o runtime da plataforma de destino a partir dos jmods dela.
jlink --module-path "$JMODS" --add-modules "$MODULOS_JAVA" --include-locales=pt-BR,en \
  --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
  --output "$DESTINO/java"

# ---- PostgreSQL ------------------------------------------------------------------------------
echo "==> PostgreSQL 17 ($PLATAFORMA)"
if [ "$PLATAFORMA" = windows ]; then
  baixar "$PG_WINDOWS_ARQUIVO" "$PG_WINDOWS_URL" "$PG_WINDOWS_SHA256"
  # Somente o servidor e as ferramentas de linha de comando (sem pgAdmin, StackBuilder, docs e
  # cabeçalhos de compilação).
  extrair_zip "$CACHE/$PG_WINDOWS_ARQUIVO" "$TEMP/pg" pgsql/bin/ pgsql/lib/ pgsql/share/ \
    pgsql/server_license.txt pgsql/commandlinetools_3rd_party_licenses.txt
  rm -f "$TEMP/pg/pgsql/bin/stackbuilder.exe"
  find "$TEMP/pg/pgsql/lib" -name '*.lib' -delete
  # O zip não guarda permissões Unix; o bit de execução permite rodar os .exe pelo WSL (testes).
  chmod +x "$TEMP"/pg/pgsql/bin/*.exe
  mv "$TEMP/pg/pgsql" "$DESTINO/postgresql"
else
  docker pull -q "$PG_LINUX_IMAGEM" >/dev/null
  CONTAINER="$(docker create "$PG_LINUX_IMAGEM")"
  mkdir -p "$TEMP/pg/lib/postgresql" "$TEMP/pg/share/postgresql" "$TEMP/pg/bibliotecas"
  docker cp -L "$CONTAINER:/usr/lib/postgresql/17" "$TEMP/pg/lib/postgresql/"
  docker cp -L "$CONTAINER:/usr/share/postgresql/17" "$TEMP/pg/share/postgresql/"
  # No Debian, share/postgresql/17/postgresql.conf.sample é um link para ../postgresql.conf.sample.
  rm -f "$TEMP/pg/share/postgresql/17/postgresql.conf.sample"
  docker cp -L "$CONTAINER:/usr/share/postgresql/postgresql.conf.sample" "$TEMP/pg/share/postgresql/17/"
  # Bibliotecas usadas pelos executáveis e módulos, exceto as da glibc (presentes em qualquer
  # Linux) e as do LLVM (o JIT fica desligado e o módulo llvmjit não é empacotado).
  BIBLIOTECAS="$(docker run --rm --entrypoint sh "$PG_LINUX_IMAGEM" -c '
    ldd /usr/lib/postgresql/17/bin/* /usr/lib/postgresql/17/lib/*.so 2>/dev/null \
      | awk "/=> \\// {print \$3}" | sort -u \
      | grep -vE "/(libc|libm|libdl|libpthread|librt|libresolv|libutil|libanl|ld-linux[^/]*|libgcc_s|libLLVM[^/]*)\.so"')"
  for biblioteca in $BIBLIOTECAS; do
    docker cp -L "$CONTAINER:$biblioteca" "$TEMP/pg/bibliotecas/"
  done
  docker rm "$CONTAINER" >/dev/null
  rm -rf "$TEMP/pg/lib/postgresql/17/lib/llvmjit"* "$TEMP/pg/lib/postgresql/17/lib/bitcode"
  mv "$TEMP/pg" "$DESTINO/postgresql"
fi

# O jlink gera arquivos somente-leitura; o empacotador do Tauri precisa poder sobrescrevê-los.
chmod -R u+w "$DESTINO"

echo "==> Pronto: $DESTINO"
du -sh "$DESTINO"/*
