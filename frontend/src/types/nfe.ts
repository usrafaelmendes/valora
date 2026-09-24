/**
 * Contratos de NF-e conforme NfeController, NfeResponse e NfeResumoResponse.
 *
 * Os valores tributários são os originais do XML (null quando ausentes na nota), e não
 * créditos calculados. Valores decimais chegam como número JSON; instantes, como ISO-8601.
 */

/** Linha da listagem de NF-e importadas (sem os itens). */
export interface NfeResumo {
  id: number;
  chaveAcesso: string;
  numero: number;
  serie: number;
  dataEmissao: string;
  naturezaOperacao: string;
  fornecedorId: number;
  fornecedorRazaoSocial: string;
  valorTotal: number | null;
  importadoEm: string;
}

export interface NfeTotais {
  valorProdutos: number | null;
  valorFrete: number | null;
  valorSeguro: number | null;
  valorDesconto: number | null;
  valorOutrasDespesas: number | null;
  valorIpi: number | null;
  valorTotal: number | null;
}

export interface NfeIcms {
  /** Tag orig (0 a 8). */
  origem: string | null;
  cst: string | null;
  csosn: string | null;
  baseCalculo: number | null;
  aliquota: number | null;
  valor: number | null;
}

export interface NfeTributo {
  cst: string | null;
  baseCalculo: number | null;
  aliquota: number | null;
  valor: number | null;
}

export interface NfeItem {
  id: number;
  numeroItem: number;
  /** null quando o item não foi vinculado a um produto cadastrado. */
  produto: { id: number; nome: string } | null;
  codigoProdutoFornecedor: string | null;
  gtin: string | null;
  descricao: string | null;
  ncm: string | null;
  cfop: string | null;
  unidade: string | null;
  quantidade: number | null;
  valorUnitario: number | null;
  valorProduto: number | null;
  valorFrete: number | null;
  valorSeguro: number | null;
  valorDesconto: number | null;
  valorOutrasDespesas: number | null;
  icms: NfeIcms;
  ipi: NfeTributo;
  pis: NfeTributo;
  cofins: NfeTributo;
}

export interface Nfe {
  id: number;
  chaveAcesso: string;
  numero: number;
  serie: number;
  dataEmissao: string;
  naturezaOperacao: string;
  fornecedor: { id: number; razaoSocial: string };
  emitenteCnpj: string;
  emitenteUf: string;
  destinatarioCnpj: string | null;
  destinatarioUf: string | null;
  totais: NfeTotais;
  importadoEm: string;
  itensSemProduto: number;
  itens: NfeItem[];
}

/**
 * Códigos da tag orig (origem da mercadoria) conforme a tabela do leiaute da NF-e.
 * Servem só para exibição: quais origens cada regra considera é configuração do ADMIN.
 */
export const ORIGENS_MERCADORIA = ['0', '1', '2', '3', '4', '5', '6', '7', '8'] as const;

export const DESCRICAO_ORIGEM_MERCADORIA: Record<string, string> = {
  '0': 'Nacional, exceto as indicadas nos códigos 3, 4, 5 e 8',
  '1': 'Estrangeira — importação direta, exceto a indicada no código 6',
  '2': 'Estrangeira — adquirida no mercado interno, exceto a indicada no código 7',
  '3': 'Nacional, com conteúdo de importação superior a 40% e inferior ou igual a 70%',
  '4': 'Nacional, produzida conforme processos produtivos básicos',
  '5': 'Nacional, com conteúdo de importação inferior ou igual a 40%',
  '6': 'Estrangeira — importação direta, sem similar nacional (lista CAMEX)',
  '7': 'Estrangeira — adquirida no mercado interno, sem similar nacional (lista CAMEX)',
  '8': 'Nacional, com conteúdo de importação superior a 70%',
};
