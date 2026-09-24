# Regras Tributárias e de Cálculo

## 1. Objetivo

Este documento descreve o motor de cálculo do Valora: como o custo efetivo das opções de fornecedores é calculado a partir dos créditos tributários aplicáveis.

O objetivo é permitir que a comparação considere os créditos tributários aplicáveis, evitando que a decisão seja baseada somente no valor nominal da nota fiscal.

O Valora **não traz regras tributárias pré-configuradas**. As regras e os parâmetros que se aplicam a cada operação são configurados pelo administrador (ADMIN), sem alteração de código. Este documento descreve o mecanismo; não define alíquotas nem regras fiscais de nenhuma empresa.

---

## 2. Escopo

O Valora é uma ferramenta de apoio à decisão de compra. Ele não é um sistema fiscal ou contábil e não substitui a orientação de um profissional da área tributária.

A responsabilidade pelas regras configuradas (alíquotas, condições e prioridades) é de quem as configura.

---

## 3. Tributos Considerados

O motor calcula créditos de:

* ICMS;
* IPI;
* PIS;
* COFINS;
* PIS/COFINS combinado (`PIS_COFINS`), quando a regra configurada tratar os dois como um único crédito.

A presença de um tributo nesta lista não significa que seu crédito será aplicado automaticamente: o crédito só existe quando há uma regra configurada que se aplique à operação.

---

## 4. Conceito de Custo Efetivo

O sistema não considera somente o preço nominal apresentado pelo fornecedor. O cálculo considera o valor da operação e os créditos tributários aplicáveis:

**Custo Efetivo = Valor da Operação − Total de Créditos Tributários Aplicáveis**

Cada crédito é calculado **individualmente sobre a base de cálculo** (por padrão, o valor da operação). Os créditos são somados e o total é subtraído do valor da operação.

Os percentuais **não são aplicados em cascata** (cada desconto sobre o resultado do anterior).

### Exemplo (valores fictícios)

Operação de R$ 325,00 com regras configuradas de crédito de ICMS de 7%, IPI de 10% e PIS/COFINS de 6,35%:

* Crédito de ICMS: 325 × 7% = R$ 22,75;
* Crédito de IPI: 325 × 10% = R$ 32,50;
* Crédito de PIS/COFINS: 325 × 6,35% = R$ 20,6375 (R$ 20,64 arredondado);
* Total de créditos: R$ 75,89;
* Custo Efetivo: R$ 325,00 − R$ 75,89 = R$ 249,11.

Em cascata, o resultado seria R$ 254,75, que **não** corresponde ao cálculo do Valora.

O arredondamento dos créditos (por crédito ou somente no total, e o critério de desempate no meio) é um parâmetro configurável (§11).

---

## 5. Valor da Operação e Base dos Créditos

O valor da operação e a base dos créditos são definidos por parâmetros (§11):

* de onde vêm os valores: do item da NF-e importada ou dos valores informados (ex.: na cotação);
* de onde vêm os dados fiscais (origem da mercadoria, CFOP, alíquotas destacadas): do item da NF-e, dos dados informados ou da NF-e mais recente do mesmo fornecedor e produto;
* quais componentes formam o valor da operação (valor dos produtos, IPI, frete, seguro, outras despesas e desconto);
* qual é a base dos créditos: o valor da operação ou uma lista de componentes.

O resultado permite identificar, sempre que possível: valor da operação, créditos considerados, total de créditos e custo efetivo resultante.

---

## 6. Tipo de Fornecedor e Fator

O cadastro do fornecedor registra o tipo (fabricante ou atacadista/revendedor). Uma regra pode ser condicionada ao tipo de fornecedor.

Cada regra tem um **fator**, que multiplica a alíquota obtida. Exemplo fictício: com alíquota obtida de 10% e fator 0,25, a alíquota aplicada é 2,5%. O fator permite configurar créditos parciais sem alterar o código.

---

## 7. UF de Origem e de Destino

Uma regra pode ser condicionada às UFs da operação:

* UF de origem e UF de destino específicas;
* abrangência: operação **interna** (origem e destino na mesma UF) ou **interestadual** (origens e destinos diferentes).

A UF de origem corresponde ao estado de emissão da NF-e (emitente do XML) ou, conforme parâmetro, à UF do cadastro do fornecedor. A UF de destino das operações é um parâmetro configurado pelo ADMIN (§11).

O sistema não assume uma única alíquota fixa para operações interestaduais: a alíquota vem da regra configurada (percentual fixo ou alíquota destacada na NF-e).

---

## 8. Condição de Pagamento

A condição de pagamento de cada opção e o prazo de pagamento base do fornecedor são armazenados e apresentados na comparação.

A condição de pagamento **não altera** matematicamente o custo efetivo.

---

## 9. Regras Configuráveis

Cada regra define **como obter a alíquota de crédito de um tributo** e **em quais operações ela vale**:

| Campo | Significado |
|---|---|
| `tributo` | `ICMS`, `IPI`, `PIS`, `COFINS` ou `PIS_COFINS` (crédito combinado) |
| `formaAliquota` | `PERCENTUAL_FIXO` (usa a `aliquota` da regra), `ALIQUOTA_DA_NFE` (usa a alíquota destacada nos dados fiscais da operação) ou `SEM_CREDITO` (crédito zero declarado) |
| `aliquota` | Percentual, somente para `PERCENTUAL_FIXO` |
| `fator` | Multiplicador da alíquota obtida (§6) |
| `prioridade` | Entre regras aplicáveis ao mesmo tributo, vence a maior; empate = conflito, e o sistema não escolhe |
| `ativa` | Somente regras ativas são consideradas |
| Condições | `tipoFornecedor`, `fornecedorId`, `produtoId`, `ufOrigem`, `ufDestino`, `abrangenciaUf` (`INTERNA`/`INTERESTADUAL`), `origensMercadoria` (tag `orig` da NF-e, 0 a 8), `cfops`. Condição vazia = qualquer valor |

Para cada tributo da operação, o resultado da seleção é:

* **aplicável**: uma regra atende às condições e tem a maior prioridade;
* **sem regra**: nenhuma regra ativa atende às condições. O sistema não presume crédito zero, e o cálculo fica incompleto;
* **conflito**: mais de uma regra com a mesma maior prioridade. O sistema não escolhe, e o cálculo fica incompleto.

O ADMIN confere a configuração pela conferência de regras aplicáveis, que mostra, para uma operação de exemplo, qual regra seria usada em cada tributo.

Uma regra alterada ganha uma nova versão; cálculos já gravados mantêm a versão que usaram. Uma regra não é apagada: é desativada.

---

## 10. Rastreabilidade do Cálculo

O resultado da comparação permite identificar quais valores e créditos foram considerados no cálculo. Cada cálculo registra:

* valor da operação e base dos créditos;
* crédito de cada tributo, com a regra usada (id, versão, nome e forma), a alíquota obtida, o fator, a alíquota aplicada, o valor sem arredondamento e o valor arredondado;
* total de créditos e diferença de arredondamento;
* custo efetivo;
* fornecedor e condição de pagamento;
* parâmetros vigentes, pendências e avisos.

Alterações posteriores nas regras ou nos parâmetros não mudam cálculos já gravados.

---

## 11. Parâmetros Gerais do Cálculo

Parâmetros configurados pelo ADMIN. Um parâmetro sem valor fica "não definido": o cálculo informa a pendência em vez de presumir um valor.

| Chave | Significado | Valor inicial |
|---|---|---|
| `UF_DESTINO` | UF de destino das operações | não definido |
| `FONTE_UF_ORIGEM` | UF de origem pelo emitente da NF-e ou pelo cadastro do fornecedor | `EMITENTE_NFE` |
| `FONTE_VALORES_OPERACAO` | Valores do item da NF-e ou informados | não definido |
| `FONTE_DADOS_FISCAIS` | Dados fiscais do item da NF-e, informados ou da última NF-e do fornecedor e produto | não definido |
| `COMPOSICAO_VALOR_OPERACAO` | Componentes do valor da operação | não definido |
| `COMPOSICAO_BASE_CREDITOS` | Base dos créditos | `VALOR_OPERACAO` |
| `ARREDONDAMENTO_CREDITOS` | Arredondar cada crédito ou somente o total | não definido |
| `CRITERIO_ARREDONDAMENTO` | Meio para cima ou meio para o par | não definido |
| `CFOPS_PARTICIPANTES` | CFOPs que participam da comparação | não definido |

Com a configuração inicial, os cálculos ficam **incompletos** e informam quais parâmetros ou regras faltam.

---

## 12. Limitações

O sistema não pretende substituir um sistema fiscal ou contábil especializado.

O Valora calcula créditos de ICMS, IPI, PIS e COFINS conforme as regras configuradas. Outros tributos e situações não previstas pelas regras configuradas não entram no cálculo.

Valores provisórios podem ser usados em testes, desde que identificados como dados de teste e não como regras fiscais.
