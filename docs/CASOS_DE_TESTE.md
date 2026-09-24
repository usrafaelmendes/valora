# Casos de Teste — Valora

## 1. Objetivo

Este documento define os principais casos de teste do MVP.

Os testes deverão validar o funcionamento das funcionalidades e, principalmente, garantir que o cálculo do custo efetivo e a comparação entre fornecedores estejam corretos.

Os valores tributários utilizados neste documento são **fictícios**, configurados apenas para testar o motor de cálculo. Não representam regras fiscais de nenhuma empresa.

---

## 2. Convenções

Cada caso de teste deverá possuir:

- identificador;
- descrição;
- pré-condições;
- entrada;
- resultado esperado.

Status possíveis:

- PENDENTE;
- APROVADO;
- REPROVADO;
- BLOQUEADO.

---

## 3. Autenticação

### CT01 — Login válido

**Pré-condição:**
Usuário cadastrado e ativo.

**Entrada:**
Credenciais válidas.

**Resultado esperado:**
O sistema autentica o usuário e permite acesso às funcionalidades correspondentes ao seu perfil.

**Status:**
APROVADO (Etapa 15) — E2E (Playwright) contra PostgreSQL real: login de ADMIN e USER pela interface; AutenticacaoControllerTest.

---

### CT02 — Login inválido

**Entrada:**
Senha incorreta.

**Resultado esperado:**
O sistema rejeita a autenticação e informa que as credenciais são inválidas.

**Status:**
APROVADO (Etapa 15) — E2E: senha incorreta retorna 401; AutenticacaoControllerTest e testes do frontend (mensagem de credenciais inválidas).

---

### CT03 — Acesso administrativo

**Pré-condição:**
Usuário com perfil ADMIN.

**Resultado esperado:**
O usuário consegue acessar funcionalidades administrativas.

**Status:**
APROVADO (Etapa 15) — E2E: ADMIN acessa NF-e, regras, parâmetros e usuários (200) e usa as telas administrativas, inclusive a tela Usuários (CT38).

---

### CT04 — Bloqueio de acesso administrativo

**Pré-condição:**
Usuário com perfil USER.

**Resultado esperado:**
O usuário não consegue acessar funcionalidades exclusivas do administrador.

**Status:**
APROVADO (Etapa 15) — E2E: USER recebe 403 em produtos/fornecedores (escrita), parâmetros, regras, NF-e (inclusive upload) e usuários (listagem e criação), não vê o item Usuários no menu e vê "Acesso restrito" nas rotas administrativas, inclusive `/usuarios`.

---

### CT37 — Configuração inicial (primeiro ADMIN)

**Pré-condição:**
Banco sem nenhum usuário (instalação nova).

**Entrada:**
Nome, e-mail, senha e confirmação da senha do primeiro administrador.

**Resultado esperado:**
O sistema direciona para a configuração inicial, cria o primeiro usuário com perfil ADMIN (senha em BCrypt) e, a partir daí, a configuração inicial fica indisponível: novas tentativas são recusadas, inclusive quando simultâneas (somente um ADMIN é criado). Dados inválidos (nome vazio, e-mail inválido, senha fora de 8–72 caracteres, confirmação diferente) são rejeitados.

**Status:**
APROVADO — ConfiguracaoInicialControllerTest; ConfiguracaoInicialConcorrenciaTest (requisições simultâneas contra PostgreSQL real, banco temporário); testes do frontend (ConfiguracaoInicialPage); E2E: banco vazio → configuração inicial → login do ADMIN → configuração indisponível.

---

### CT38 — Gerenciamento de usuários pelo ADMIN

**Pré-condição:**
Sistema configurado (primeiro ADMIN criado pela configuração inicial, CT37) e ADMIN autenticado.

**Entrada:**
Nome, e-mail, senha, confirmação da senha e perfil (ADMIN ou USER) do novo usuário, na tela Usuários.

**Resultado esperado:**
O ADMIN consulta os usuários cadastrados (nome, e-mail, perfil e situação, sem senha) e cadastra novos usuários com perfil USER ou ADMIN (`POST /usuarios`). Dados inválidos (campos vazios, e-mail inválido, senha fora de 8–72 caracteres, confirmação diferente, perfil não escolhido) são rejeitados e e-mail já cadastrado é recusado (`409`). O usuário criado consegue entrar. A tela e o item de menu não existem para o USER, que recebe "Acesso restrito" pela URL e `403` da API. A configuração inicial continua sendo o único meio de criar o primeiro ADMIN.

**Status:**
APROVADO — testes do frontend (UsuariosPage, validacaoUsuario, permissões de rota); E2E: banco vazio → configuração inicial → ADMIN cria um USER pela tela (inclusive tentativa com e-mail duplicado) → USER entra, não acessa Usuários e recebe 403 → ADMIN continua acessando Usuários.

---

## 4. Fornecedores

### CT05 — Cadastro de fornecedor

**Entrada:**
Dados válidos de fornecedor.

**Resultado esperado:**
O fornecedor é cadastrado, com CNPJ, UF de emissão, tipo (fabricante ou atacadista) e prazo de pagamento base, e pode ser consultado posteriormente.

**Status:**
APROVADO (Etapa 15) — E2E: cadastro pela interface com CNPJ, UF, tipo e prazo base; consulta posterior como USER.

---

### CT06 — Validação de fornecedor

**Entrada:**
Dados obrigatórios ausentes ou inválidos.

**Resultado esperado:**
O sistema rejeita o cadastro e informa os campos que precisam ser corrigidos.

**Status:**
APROVADO (Etapa 15) — FornecedorControllerTest (campos obrigatórios/inválidos, erros por campo) e testes do formulário no frontend.

---

## 5. Produtos

### CT07 — Cadastro de produto

**Entrada:**
Dados válidos de produto.

**Resultado esperado:**
O produto é cadastrado (por exemplo, "Produto Teste A") e pode ser utilizado em uma cotação.

**Status:**
APROVADO (Etapa 15) — E2E: produto cadastrado pela interface e usado na cotação.

---

### CT08 — Validação de produto

**Entrada:**
Dados obrigatórios inválidos.

**Resultado esperado:**
O sistema rejeita o cadastro.

**Status:**
APROVADO (Etapa 15) — ProdutoControllerTest (ct08_*) e testes do formulário no frontend.

---

## 6. Importação de NF-e

### CT09 — Importação de XML válido

**Entrada:**
Arquivo XML de NF-e válido.

**Resultado esperado:**
O sistema aceita o arquivo, processa o XML e armazena as informações relevantes.

**Status:**
APROVADO (Etapa 15) — E2E: importação do XML sintético (backend/src/test/resources/nfe) pela interface, itens e tributos exibidos; NfeControllerTest. Nenhuma NF-e real usada.

---

### CT10 — XML inválido

**Entrada:**
Arquivo que não corresponde a uma NF-e XML válida.

**Resultado esperado:**
O sistema rejeita o arquivo e informa o erro.

**Status:**
APROVADO (Etapa 15) — E2E: XML que não é NF-e rejeitado com erro de validação; NfeControllerTest e NfeXmlParserTest (inclusive entidade externa/XXE).

---

### CT11 — XML com informações obrigatórias ausentes

**Entrada:**
NF-e com dados necessários ao sistema ausentes ou inválidos.

**Resultado esperado:**
O sistema identifica o problema e não utiliza dados inválidos no cálculo.

**Status:**
APROVADO (Etapa 15) — NfeControllerTest (ct11_campoObrigatorioAusenteRetorna400) e NfeXmlParserTest (campos tributários ausentes ficam nulos, nunca zero).

---

## 7. Cálculo do Custo Efetivo

### CT12 — Comparação baseada no custo efetivo

**Objetivo:**
Garantir que o sistema compare fornecedores considerando o custo efetivo da aquisição, e não somente o valor nominal da NF-e.

**Cenário:**

Uma NF-e possui valor de operação de R$ 325,00.

Para fins de teste, considere as seguintes regras fictícias de crédito configuradas:

* ICMS: 7%;
* IPI: 10%;
* PIS/COFINS: 6,35%.

Os créditos deverão ser calculados individualmente sobre o valor da operação:

* crédito de ICMS: R$ 22,75;
* crédito de IPI: R$ 32,50;
* crédito de PIS/COFINS: R$ 20,64;
* total de créditos: R$ 75,89.

Custo efetivo:

**R$ 325,00 − R$ 75,89 = R$ 249,11**

**Resultado esperado:**
O sistema deverá calcular o custo efetivo considerando os créditos tributários aplicáveis e utilizar esse valor na comparação entre fornecedores.

**Observação:**
Os valores deste caso são fictícios.

O valor de PIS/COFINS antes do arredondamento é R$ 20,6375. O arredondamento é definido pelos parâmetros ARREDONDAMENTO_CREDITOS e CRITERIO_ARREDONDAMENTO.

**Status:**
PENDENTE

---

### CT13 — Sem crédito tributário

**Objetivo:**
Garantir que o sistema consiga calcular o custo efetivo quando não houver crédito tributário aplicável.

**Entrada:**
NF-e com valor de operação de R$ 325,00 e nenhum crédito tributário aplicável.

**Resultado esperado:**
O total de créditos deverá ser R$ 0,00 e o custo efetivo deverá permanecer igual ao valor da operação:

R$ 325,00 − R$ 0,00 = R$ 325,00

O sistema não deverá inventar ou aplicar créditos que não estejam previstos para aquela operação.

**Status:**

PENDENTE

---

### CT14 — Crédito tributário aplicável

**Entrada:**
Fornecedor com crédito tributário aplicável.

**Resultado esperado:**
O crédito deverá ser considerado de acordo com a regra tributária configurada para a operação.

Quando houver mais de um tributo aplicável, cada crédito deverá ser calculado individualmente sobre o valor da operação e considerado no cálculo do custo efetivo.

**Status:**
PENDENTE

---

### CT15 — Regra por tipo de fornecedor com fator

**Entrada:**
Fornecedor classificado como atacadista.

**Resultado esperado:**
O cálculo deverá aplicar a regra de IPI configurada para o tipo de fornecedor atacadista, multiplicando a alíquota obtida pelo fator configurado na regra.

**Observação:**
A alíquota efetiva do IPI deverá ser obtida conforme os dados da operação e permanecer configurável.

Exemplo de teste (valores fictícios): com alíquota obtida de 10%, fator 0,25 e valor de operação de R$ 325,00, o crédito de IPI do atacadista é 325 × 2,5% = R$ 8,125.

**Status:**
PENDENTE

---

### CT16 — Operação interestadual

**Entrada:**
Produto proveniente de outro estado.

**Resultado esperado:**
O cálculo deverá considerar o crédito de ICMS conforme a regra interestadual configurada, com base no estado de origem (UF de emissão da NF-e) e no estado de destino (parâmetro UF_DESTINO).

**Status:**
PENDENTE

---

### CT17 — Regra tributária não definida

**Entrada:**
Situação tributária para a qual não existe regra configurada.

**Resultado esperado:**
O sistema não deverá inventar uma regra.

Deverá informar que a situação precisa de configuração ou validação.

**Status:**
PENDENTE

---

## 8. Comparação de Fornecedores

### CT18 — Ordenação das opções

**Entrada:**
Duas ou mais opções de fornecedores com custos efetivos diferentes.

**Resultado esperado:**
As opções deverão ser apresentadas ordenadas de acordo com o custo efetivo calculado.

Caso existam critérios adicionais, eles somente deverão ser utilizados para desempate ou ordenação quando houver uma regra de decisão formalmente documentada.

**Status:**
APROVADO (Etapa 15) — E2E e ComparadorAlternativasTest: ordem pelo custo efetivo calculado no backend, exibida sem reordenação. Nenhum critério adicional (ex.: prazo) está definido.

---

### CT19 — Exibição das alternativas

**Entrada:**
Múltiplos fornecedores para o mesmo produto.

**Resultado esperado:**
O sistema deverá apresentar todas as opções relevantes, e não somente a primeira colocada.

**Status:**
APROVADO (Etapa 15) — E2E: todas as alternativas classificadas e as não classificadas (com motivo) são exibidas.

---

### CT20 — Custos efetivos iguais

**Entrada:**
Duas opções com o mesmo custo efetivo.

**Resultado esperado:**
O sistema deverá tratar o empate de forma determinística e apresentar as informações necessárias para comparação.

**Status:**
APROVADO (Etapa 15) — E2E e ComparadorAlternativasTest: empate marcado e desempate técnico determinístico (ordem de cadastro), registrado como critério.

---

## 9. Condição de Pagamento

### CT21 — Exibição da condição de pagamento

**Entrada:**
Fornecedor com condição de pagamento informada.

**Resultado esperado:**
A condição de pagamento deverá aparecer na comparação.

**Status:**
APROVADO (Etapa 15) — E2E: condição informada e prazo base do fornecedor exibidos na comparação e no CSV.

---

### CT22 — Condição de pagamento ausente

**Entrada:**
Fornecedor sem condição de pagamento informada.

**Resultado esperado:**
O sistema deverá tratar a informação como ausente sem causar erro no cálculo.

**Status:**
APROVADO (Etapa 15) — CotacaoControllerTest (condição em branco tratada como ausente) e testes do frontend ("Não informado"), sem erro no cálculo.

---

## 10. Cotação

### CT23 — Criação de cotação

**Entrada:**
Produto e opções de fornecedores válidos.

**Resultado esperado:**
O sistema cria a cotação e calcula as opções disponíveis.

**Status:**
APROVADO (Etapa 15) — E2E: USER cria a cotação e o backend executa a comparação inicial.

---

### CT24 — Visualização da cotação

**Resultado esperado:**
A tabela deverá apresentar, quando disponíveis:

- produto;
- fornecedor;
- preço;
- créditos considerados;
- custo efetivo;
- condição de pagamento;
- posição da opção.

**Status:**
APROVADO (Etapa 15) — E2E: produto, fornecedor, preço, créditos por tributo, custo efetivo, condição de pagamento e posição exibidos.

---

### CT25 — Download da cotação

**Entrada:**
Cotação processada.

**Resultado esperado:**
O usuário consegue baixar a tabela da cotação.

**Status:**
APROVADO (Etapa 15) — E2E: download do CSV com nome comparacao-{id}-cotacao-{id}.csv e conteúdo conferido, sem criar nova comparação.

---

## 11. Banco de Dados

### CT26 — Persistência

**Entrada:**
Cadastro ou processamento válido.

**Resultado esperado:**
Os dados são armazenados corretamente no PostgreSQL.

**Status:**
APROVADO (Etapa 15) — E2E em banco PostgreSQL temporário, com schema criado pelo Flyway (V1 a V8).

---

### CT27 — Recuperação dos dados

**Entrada:**
Dados previamente armazenados.

**Resultado esperado:**
O sistema consegue recuperar e apresentar os dados corretamente.

**Status:**
APROVADO (Etapa 15) — E2E: dados gravados são consultados em outras telas e sessões (USER), inclusive o histórico de comparações.

---

## 12. API

### CT28 — Endpoint válido

**Entrada:**
Requisição válida para um endpoint existente.

**Resultado esperado:**
A API retorna resposta adequada e os dados esperados.

**Status:**
APROVADO (Etapa 15) — E2E e testes de controller: respostas 200/201 com os dados esperados.

---

### CT29 — Requisição inválida

**Entrada:**
Dados inválidos ou incompletos.

**Resultado esperado:**
A API rejeita a requisição e retorna uma resposta de erro adequada.

**Status:**
APROVADO (Etapa 15) — testes de controller (400 com erros por campo) e E2E (XML inválido, 401 e 403).

---

## 13. Testes de Integração

### CT30 — Fluxo completo de NF-e até comparação

**Fluxo:**

1. Administrador realiza login.
2. Administrador cadastra fornecedor.
3. Administrador cadastra produto.
4. Administrador importa uma NF-e.
5. Sistema processa o XML.
6. Sistema armazena os dados.
7. Usuário realiza uma cotação.
8. Sistema aplica as regras.
9. Sistema calcula os custos efetivos.
10. Sistema apresenta a comparação.

**Resultado esperado:**
O fluxo completo deverá funcionar sem inconsistências.

**Status:**
PENDENTE — o fluxo foi executado no E2E (Etapa 15) até a comparação, mas com os valores informados na cotação. Usar os dados da NF-e importada no cálculo depende dos parâmetros FONTE_VALORES_OPERACAO, FONTE_DADOS_FISCAIS e CFOPS_PARTICIPANTES, configurados pelo ADMIN.

---

## 14. Testes de Regressão

Após alterações importantes, os testes relacionados às funcionalidades afetadas deverão ser executados novamente.

O objetivo é garantir que novas alterações não quebrem funcionalidades previamente aprovadas.

---

## 15. Dados Reais e Dados de Teste

Dados reais de empresas não são usados nos testes automatizados nem versionados.

Durante o desenvolvimento, deverá ser priorizado o uso de dados fictícios ou dados de teste.

Os testes automatizados usam o XML sintético em:

`backend/src/test/resources/nfe/`

Informações sensíveis não deverão ser versionadas no Git.

---

## 16. Validação Tributária

Os testes tributários verificam o motor de cálculo com regras fictícias. A validação das regras fiscais de cada operação é responsabilidade de quem as configura.

Os casos tributários ainda não validados em ambiente real permanecem como:

**PENDENTE**

Nenhum resultado provisório deverá ser tratado como validação fiscal definitiva.

---

## 17. Critério de Aceitação do MVP

O MVP deverá possuir testes aprovados para o fluxo principal:

1. autenticação;
2. cadastro;
3. importação de NF-e;
4. processamento;
5. cálculo;
6. comparação;
7. apresentação do resultado;
8. download da cotação.

Os casos tributários que dependerem de configuração específica deverão ser executados com as regras configuradas para cada operação.

---

## 18. Atualização dos Casos de Teste

Novos casos de teste deverão ser adicionados quando:

- uma nova regra de negócio for definida;
- uma nova regra tributária for confirmada;
- uma nova funcionalidade relevante for implementada;
- um erro importante for identificado;
- uma alteração puder afetar funcionalidades existentes.

## 19. Testes Tributários Adicionais

### CT31 — Regra por origem da mercadoria

**Entrada:**
Operação cuja origem da mercadoria (tag `orig` da NF-e) está na lista de origens de uma regra configurada.

**Resultado esperado:**
O cálculo deverá usar a regra condicionada à origem da mercadoria, quando ela tiver a maior prioridade entre as regras aplicáveis. Exemplo fictício: regra de ICMS de 5% para as origens 1 e 2, com prioridade maior que a regra interestadual.

**Observação:**
A origem da mercadoria vem dos dados fiscais da operação (NF-e ou informados).

**Status:**
PENDENTE

---

### CT33 — IPI por tipo de fornecedor: fabricante versus atacadista

**Entrada:**
O mesmo item, com alíquota de IPI da operação de 10% e valor de operação de R$ 325,00, cotado com um fabricante e com um atacadista, com regras fictícias: fabricante com fator 1 e atacadista com fator 0,25.

**Resultado esperado:**
Fabricante: crédito de IPI de R$ 32,50. Atacadista: crédito de IPI de R$ 8,125 (alíquota aplicada de 2,5%), sujeito ao arredondamento configurado.

**Status:**
PENDENTE

---

### CT34 — Estado de origem obtido pelo CNPJ

**Entrada:**
Fornecedor com CNPJ válido, emitente de NF-e de outro estado.

**Resultado esperado:**
O sistema identifica o estado de origem a partir do CNPJ do fornecedor e o utiliza na regra de ICMS interestadual, com a UF de destino configurada.

**Status:**
PENDENTE

---

### CT35 — Prazo de pagamento não altera o custo efetivo

**Entrada:**
Duas opções com o mesmo valor de operação e os mesmos créditos, uma com prazo de 30 dias e outra com prazo de 90 dias.

**Resultado esperado:**
Os custos efetivos permanecem iguais. Os prazos são exibidos na comparação, e a ordenação segue as regras de RF11 e CT20 (empate determinístico), pois não há fórmula ou regra de decisão validada para o prazo.

**Status:**
PENDENTE

---

### CT36 — Créditos calculados sobre o todo, sem cascata

**Entrada:**
Valor de operação de R$ 325,00 com créditos fictícios de ICMS 7%, IPI 10% e PIS/COFINS 6,35%.

**Resultado esperado:**
Custo efetivo de R$ 249,11. O sistema não deve retornar R$ 254,75, que resultaria da aplicação dos percentuais em cascata.

**Status:**
PENDENTE

---
