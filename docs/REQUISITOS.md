# Requisitos do Sistema — Valora

## 1. Objetivo

O Valora tem como objetivo auxiliar empresas na comparação de fornecedores durante o processo de cotação de produtos.

A comparação não deve considerar apenas o preço nominal informado pelo fornecedor. O sistema deve calcular o custo efetivo do produto considerando os créditos tributários aplicáveis de acordo com as regras configuradas no sistema pelo administrador.

O resultado deve apresentar as opções de fornecedores ordenadas da mais vantajosa para a menos vantajosa, permitindo que o usuário tenha uma visão clara das alternativas disponíveis.

Em um processo de compra típico, a empresa faz orçamentos com fornecedores fabricantes ou revendedores do item cotado. Para encontrar o melhor preço, calcula o preço base (valor de nota), subtraindo do valor da nota os créditos tributários aplicáveis, e também considera a condição de pagamento de cada opção.

---

## 2. Escopo do MVP

O MVP deverá permitir:

- autenticação de usuários;
- diferenciação entre usuário comum e administrador;
- cadastro e gerenciamento de fornecedores;
- cadastro e gerenciamento de produtos;
- upload manual de arquivos XML de NF-e pelo administrador;
- leitura das informações relevantes presentes no XML;
- armazenamento das informações extraídas das NF-e;
- registro das regras tributárias configuradas pelo administrador;
- registro das informações necessárias para realizar uma cotação;
- cálculo do custo efetivo dos produtos;
- comparação entre diferentes fornecedores;
- ordenação das opções de acordo com o resultado do cálculo;
- visualização da melhor opção e das demais alternativas;
- geração e download de uma tabela de cotação.

---

## 3. Usuários

### 3.1 Usuário comum

O usuário comum poderá:

- realizar login;
- consultar produtos;
- realizar comparações de fornecedores;
- visualizar os resultados das cotações;
- visualizar os critérios utilizados no cálculo;
- visualizar as alternativas disponíveis;
- baixar a tabela de cotação.

O usuário comum não deverá alterar cadastros administrativos ou regras tributárias.

### 3.2 Administrador

O administrador poderá:

- realizar login;
- cadastrar, editar e consultar fornecedores;
- cadastrar, editar e consultar produtos;
- importar arquivos XML de NF-e;
- consultar as informações extraídas das NF-e;
- gerenciar as informações necessárias para os cálculos;
- gerenciar as regras tributárias configuradas no sistema;
- cadastrar e consultar usuários, com perfil ADMIN ou USER (RF01.1).

---

## 4. Requisitos Funcionais

### RF01 — Autenticação

O sistema deverá permitir que usuários autenticados acessem as funcionalidades de acordo com seu perfil de acesso.

#### RF01.1 — Configuração inicial (primeiro acesso)

Em uma instalação nova, sem nenhum usuário cadastrado, o sistema deverá direcionar para uma tela de configuração inicial, na qual o primeiro usuário ADMIN é criado informando nome, e-mail, senha e confirmação da senha.

Não deverá existir usuário ou senha administrativa padrão, nem credenciais do ADMIN em configuração do ambiente.

Assim que existir qualquer usuário, a configuração inicial deixará de estar disponível e não poderá ser usada para criar outro ADMIN; novos usuários continuarão sendo criados somente por um ADMIN autenticado.

### RF02 — Controle de acesso

O sistema deverá possuir, no mínimo, os perfis:

- ADMIN;
- USER.

As funcionalidades administrativas deverão ser restritas ao perfil ADMIN.

### RF03 — Cadastro de fornecedores

O administrador deverá poder cadastrar, consultar, editar e excluir fornecedores.

O cadastro deverá armazenar as informações necessárias para identificar o fornecedor e aplicar as regras de cálculo relacionadas a ele, entre elas:

- identificação e CNPJ;
- estado de emissão da NF-e (UF), que pode ser obtido a partir do CNPJ;
- tipo do fornecedor (fabricante ou atacadista/revendedor);
- créditos tributários aplicáveis;
- prazo de pagamento base.

### RF04 — Cadastro de produtos

O administrador deverá poder cadastrar, consultar, editar e excluir produtos.

O cadastro deverá permitir identificar corretamente o produto utilizado nas cotações e nas informações provenientes das NF-e.

Os mesmos produtos poderão ser adquiridos de diferentes fornecedores.

### RF05 — Importação de NF-e

O administrador deverá poder enviar manualmente arquivos XML de NF-e para o sistema.

O sistema deverá validar o arquivo antes de processá-lo.

### RF06 — Processamento do XML

O sistema deverá extrair do XML da NF-e as informações necessárias para o funcionamento do MVP.

Entre as informações que poderão ser utilizadas estão:

- fornecedor;
- produto;
- quantidade;
- valor do produto;
- valor da nota;
- ICMS;
- IPI;
- PIS;
- COFINS;
- informações necessárias para identificar a operação;
- demais informações fiscais necessárias ao cálculo conforme as regras configuradas.

O sistema não deverá depender de integração automática com a SEFAZ para o MVP.

### RF07 — Armazenamento das NF-e

Após o processamento, as informações relevantes da NF-e deverão ser armazenadas no banco de dados para utilização posterior.


### RF08 — Cotação

O sistema deverá permitir que o usuário selecione ou informe um produto e as opções de fornecedores que deverão ser comparadas.

As opções de fornecedores deverão possuir as informações necessárias para o cálculo, como preço de aquisição (preço da cotação) e, quando disponíveis, condição de pagamento e informações tributárias aplicáveis.

O sistema deverá utilizar essas informações juntamente com os dados históricos e tributários disponíveis para realizar a comparação.

### RF09 — Cálculo do custo efetivo

O sistema deverá calcular o custo efetivo de cada opção considerando:

- preço informado;
- créditos tributários aplicáveis;
- regras tributárias configuradas;
- demais fatores definidos para o MVP.

A fórmula exata deverá ser definida no documento `REGRAS_TRIBUTARIAS.md`.

### RF10 — Comparação de fornecedores

O sistema deverá comparar as opções disponíveis para um determinado produto.

A comparação deverá considerar o custo efetivo calculado, e não apenas o preço nominal.

### RF11 — Ordenação dos resultados

O sistema deverá apresentar as opções de fornecedores ordenadas pelo custo efetivo calculado, do menor para o maior.

Outros critérios, como condição de pagamento, eles somente deverão influenciar a ordenação caso exista uma regra de decisão formalmente definida e documentada.

### RF12 — Visualização da cotação

O sistema deverá apresentar uma tabela contendo, no mínimo:

- produto;
- fornecedor;
- preço informado;
- créditos tributários considerados;
- custo efetivo;
- condição de pagamento, quando disponível;
- posição da opção na comparação.

### RF13 — Download da cotação

O usuário deverá poder baixar a tabela de cotação apresentada pelo sistema.

O formato exato do arquivo poderá ser definido durante a implementação do MVP.

### RF14 — Histórico de informações

O sistema deverá manter as informações relevantes provenientes das NF-e importadas para que possam ser utilizadas como dados históricos.

### RF15 — Tratamento de erros

O sistema deverá informar ao usuário quando ocorrerem erros de:

- autenticação;
- validação de dados;
- upload;
- processamento de XML;
- cálculo;
- comunicação com o banco de dados.

As mensagens apresentadas ao usuário deverão ser claras e não deverão expor informações sensíveis ou detalhes internos desnecessários.

---

## 5. Requisitos Não Funcionais

### RNF01 — Tecnologia

O backend deverá ser desenvolvido em Java utilizando Spring Boot.

### RNF02 — API

O backend deverá disponibilizar uma API REST para comunicação com o frontend.

### RNF03 — Banco de dados

O sistema deverá utilizar PostgreSQL como banco de dados.

### RNF04 — Frontend

O frontend deverá ser desenvolvido como uma aplicação web.

### RNF05 — Segurança

Senhas de usuários não deverão ser armazenadas em texto puro.

O sistema deverá utilizar mecanismos adequados de autenticação e autorização.

### RNF06 — Organização

O sistema deverá possuir uma arquitetura modular, permitindo a evolução futura das funcionalidades sem necessidade de reestruturar completamente o projeto.

### RNF07 — Configuração

Informações sensíveis e configurações específicas do ambiente não deverão ser armazenadas diretamente no código-fonte.

### RNF08 — Versionamento

O código-fonte deverá ser versionado utilizando Git e hospedado no GitHub.

### RNF09 — Execução local

O sistema deverá ser capaz de ser executado localmente durante o desenvolvimento utilizando o ambiente definido para o projeto.

### RNF10 — Escopo do MVP

O sistema deverá priorizar as funcionalidades necessárias para a demonstração do processo de comparação de fornecedores.

Funcionalidades avançadas que não sejam necessárias para validar o objetivo principal do projeto não deverão ser implementadas no MVP.

---

## 6. Fora do Escopo do MVP

Não fazem parte do escopo inicial:

- integração automática com a SEFAZ;
- utilização de certificado digital para busca automática de NF-e;
- sistema fiscal completo;
- emissão de NF-e;
- escrituração fiscal;
- cálculo tributário para todos os cenários possíveis;
- automação completa do processo de compras;
- integração com ERPs externos;
- inteligência artificial para tomada automática de decisão;
- sistema financeiro completo;
- gestão completa de estoque.

Essas funcionalidades poderão ser consideradas em versões futuras, caso o projeto evolua.

---

## 7. Premissas

O MVP considera que:

- as regras tributárias utilizadas no cálculo são configuradas pelo administrador; o sistema não traz regras pré-configuradas;
- os arquivos XML de NF-e são importados manualmente;
- os dados de produtos e fornecedores são cadastrados pelo administrador;
- as informações necessárias para o cálculo estarão disponíveis nos dados fornecidos;
- o sistema será utilizado inicialmente em ambiente controlado;
- o resultado apresentado pelo sistema serve como apoio à comparação de fornecedores e não substitui sistemas fiscais ou contábeis especializados.

---

## 8. Critérios Gerais de Aceitação

O MVP será considerado funcional quando for possível:

1. realizar login;
2. cadastrar um fornecedor;
3. cadastrar um produto;
4. importar uma NF-e em XML;
5. processar e armazenar as informações relevantes da NF-e;
6. utilizar as regras tributárias configuradas;
7. informar opções de fornecedores para um produto;
8. calcular o custo efetivo de cada opção conforme as regras tributárias validadas;
9. comparar as opções;
10. apresentar as opções ordenadas pelo resultado do cálculo;
11. visualizar a melhor opção e as demais alternativas;
12. gerar e baixar a tabela da cotação.

---

## 9. Observações

Este documento define os requisitos funcionais e não funcionais do MVP.

Detalhes específicos das fórmulas, créditos tributários e regras de negócio deverão ser documentados separadamente em:

`docs/REGRAS_TRIBUTARIAS.md`

A estrutura técnica e os componentes do sistema deverão ser documentados em:

`docs/ARQUITETURA.md`
