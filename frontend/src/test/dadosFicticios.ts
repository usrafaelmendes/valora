import type {
  Alternativa,
  Calculo,
  Comparacao,
  ComparacaoResumo,
  Cotacao,
  CotacaoResumo,
  OpcaoCotacao,
  SituacaoAlternativa,
} from '../types/cotacao';
import type { Fornecedor } from '../types/fornecedor';
import type { Nfe, NfeResumo } from '../types/nfe';
import type { Produto } from '../types/produto';

/**
 * Dados fictícios para os testes das áreas administrativas. Nenhum dado real de NF-e:
 * chaves, CNPJs, valores e descrições foram inventados.
 */
export const FORNECEDOR_TESTE: Fornecedor = {
  id: 10,
  razaoSocial: 'Fornecedor Ficticio Ltda',
  cnpj: '11222333000181',
  uf: 'MG',
  tipo: 'ATACADISTA',
  prazoPagamentoBase: null,
  ativo: true,
};

export const PRODUTO_TESTE: Produto = {
  id: 20,
  nome: 'Produto Ficticio A',
  descricao: null,
  gtin: '7891234567895',
  ativo: true,
};

export const NFE_RESUMO_TESTE: NfeResumo = {
  id: 1,
  chaveAcesso: '31260911222333000181550010000001231000001234',
  numero: 123,
  serie: 1,
  dataEmissao: '2026-09-10T13:30:00Z',
  naturezaOperacao: 'Venda de mercadoria (teste)',
  fornecedorId: FORNECEDOR_TESTE.id,
  fornecedorRazaoSocial: FORNECEDOR_TESTE.razaoSocial,
  valorTotal: 325,
  importadoEm: '2026-09-11T12:00:00Z',
};

const TRIBUTO_AUSENTE = { cst: null, baseCalculo: null, aliquota: null, valor: null };

export const NFE_TESTE: Nfe = {
  id: 1,
  chaveAcesso: NFE_RESUMO_TESTE.chaveAcesso,
  numero: 123,
  serie: 1,
  dataEmissao: NFE_RESUMO_TESTE.dataEmissao,
  naturezaOperacao: NFE_RESUMO_TESTE.naturezaOperacao,
  fornecedor: { id: FORNECEDOR_TESTE.id, razaoSocial: FORNECEDOR_TESTE.razaoSocial },
  emitenteCnpj: FORNECEDOR_TESTE.cnpj,
  emitenteUf: 'MG',
  destinatarioCnpj: null,
  destinatarioUf: 'GO',
  totais: {
    valorProdutos: 200,
    valorFrete: null,
    valorSeguro: null,
    valorDesconto: null,
    valorOutrasDespesas: null,
    valorIpi: 15,
    valorTotal: 325,
  },
  importadoEm: NFE_RESUMO_TESTE.importadoEm,
  itensSemProduto: 1,
  itens: [
    {
      id: 101,
      numeroItem: 1,
      produto: { id: PRODUTO_TESTE.id, nome: PRODUTO_TESTE.nome },
      codigoProdutoFornecedor: 'COD-1',
      gtin: PRODUTO_TESTE.gtin,
      descricao: 'Item ficticio A',
      ncm: '84717012',
      cfop: '6102',
      unidade: 'UN',
      quantidade: 2,
      valorUnitario: 50.1234567891,
      valorProduto: 100,
      valorFrete: null,
      valorSeguro: null,
      valorDesconto: null,
      valorOutrasDespesas: null,
      icms: { origem: '1', cst: '00', csosn: null, baseCalculo: 100, aliquota: 7, valor: 7 },
      ipi: { cst: '50', baseCalculo: 100, aliquota: 15, valor: 15 },
      pis: { cst: '01', baseCalculo: 100, aliquota: 1.35, valor: 1.35 },
      cofins: { cst: '01', baseCalculo: 100, aliquota: 5, valor: 5 },
    },
    {
      id: 102,
      numeroItem: 2,
      produto: null,
      codigoProdutoFornecedor: 'COD-2',
      gtin: null,
      descricao: 'Cabo ficticio',
      ncm: null,
      cfop: '6102',
      unidade: 'UN',
      quantidade: 1,
      valorUnitario: 100,
      valorProduto: 100,
      valorFrete: null,
      valorSeguro: null,
      valorDesconto: null,
      valorOutrasDespesas: null,
      icms: {
        origem: null,
        cst: null,
        csosn: '102',
        baseCalculo: null,
        aliquota: null,
        valor: null,
      },
      ipi: TRIBUTO_AUSENTE,
      pis: TRIBUTO_AUSENTE,
      cofins: TRIBUTO_AUSENTE,
    },
  ],
};

/** Arquivo XML fictício para o upload (o conteúdo não é lido pelo frontend). */
export function arquivoXmlTeste(nome = 'nfe-ficticia.xml', conteudo = '<nfeProc/>'): File {
  return new File([conteudo], nome, { type: 'text/xml' });
}

/*
 * Cotação e comparação fictícias. Os valores imitam o formato gravado pelo backend (inclusive
 * posições, empates, situações e motivos) e servem apenas para verificar a apresentação:
 * o frontend não os calcula.
 */

export const FORNECEDOR_ALFA = { id: 31, razaoSocial: 'Zeta Fornecedor Ficticio Ltda' };
export const FORNECEDOR_BETA = { id: 32, razaoSocial: 'Alfa Distribuidora Ficticia SA' };
export const FORNECEDOR_GAMA = { id: 33, razaoSocial: 'Gama Pecas Ficticias ME' };

export function calculoTeste(extra: Partial<Calculo> = {}): Calculo {
  return {
    id: 501,
    status: 'CALCULADO',
    executadoEm: '2026-09-20T15:00:00Z',
    executadoPorId: 2,
    operacao: {
      nfeItemId: null,
      nfeItemDadosFiscaisId: null,
      fornecedorId: FORNECEDOR_ALFA.id,
      produtoId: PRODUTO_TESTE.id,
      tipoFornecedor: 'FABRICANTE',
      ufOrigem: 'MG',
      ufDestino: 'GO',
      origemMercadoria: '0',
      cfop: null,
      quantidade: 2,
    },
    parametros: {
      fonteValoresOperacao: 'INFORMADO',
      fonteDadosFiscais: 'INFORMADO',
      fonteUfOrigem: 'CADASTRO_FORNECEDOR',
      composicaoValorOperacao: 'VALOR_PRODUTO',
      composicaoBaseCreditos: 'VALOR_OPERACAO',
      arredondamentoCreditos: 'POR_CREDITO',
      criterioArredondamento: null,
    },
    componentes: {
      valorProduto: 325,
      valorIpi: null,
      valorFrete: 0,
      valorSeguro: null,
      valorOutrasDespesas: null,
      valorDesconto: null,
    },
    aliquotasOperacao: { icms: 7, ipi: 10, pis: null, cofins: null },
    valorOperacao: 325,
    baseCreditos: 325,
    creditos: [
      {
        tributo: 'ICMS',
        situacao: 'CALCULADO',
        regra: {
          id: 4,
          versao: 2,
          nome: 'ICMS interestadual (teste)',
          formaAliquota: 'ALIQUOTA_DA_NFE',
        },
        regrasEmConflito: null,
        aliquotaObtida: 7,
        fator: 1,
        aliquotaAplicada: 7,
        baseCalculo: 325,
        valorSemArredondamento: 22.75,
        valor: 22.75,
        mensagem: null,
      },
      {
        tributo: 'IPI',
        situacao: 'CALCULADO',
        regra: {
          id: 2,
          versao: 1,
          nome: 'IPI fabricante (teste)',
          formaAliquota: 'ALIQUOTA_DA_NFE',
        },
        regrasEmConflito: null,
        aliquotaObtida: 10,
        fator: 1,
        aliquotaAplicada: 10,
        baseCalculo: 325,
        valorSemArredondamento: 32.5,
        valor: 32.5,
        mensagem: null,
      },
      {
        tributo: 'PIS_COFINS',
        situacao: 'CALCULADO',
        regra: {
          id: 1,
          versao: 3,
          nome: 'PIS/COFINS 6,35% (teste)',
          formaAliquota: 'PERCENTUAL_FIXO',
        },
        regrasEmConflito: null,
        aliquotaObtida: 6.35,
        fator: 1,
        aliquotaAplicada: 6.35,
        baseCalculo: 325,
        valorSemArredondamento: 20.6375,
        valor: 20.64,
        mensagem: null,
      },
    ],
    totalCreditosSemArredondamento: 75.8875,
    totalCreditos: 75.89,
    diferencaArredondamento: 0.0025,
    custoEfetivo: 249.11,
    pendencias: [],
    ...extra,
  };
}

const CALCULO_INCOMPLETO: Calculo = calculoTeste({
  id: 504,
  status: 'INCOMPLETO',
  creditos: [
    {
      tributo: 'ICMS',
      situacao: 'REGRA_AUSENTE',
      regra: null,
      regrasEmConflito: null,
      aliquotaObtida: null,
      fator: null,
      aliquotaAplicada: null,
      baseCalculo: null,
      valorSemArredondamento: null,
      valor: null,
      mensagem: 'Nenhuma regra ativa se aplica ao tributo.',
    },
  ],
  totalCreditosSemArredondamento: null,
  totalCreditos: null,
  diferencaArredondamento: null,
  custoEfetivo: null,
  pendencias: [
    {
      tipo: 'REGRA_AUSENTE',
      bloqueante: true,
      mensagem: 'Nenhuma regra ativa se aplica ao ICMS (teste).',
    },
    {
      tipo: 'COMPONENTE_AUSENTE_NA_NFE',
      bloqueante: false,
      mensagem: 'Frete ausente no item, considerado zero (teste).',
    },
  ],
});

function alternativaTeste(extra: Partial<Alternativa>): Alternativa {
  return {
    posicao: null,
    empate: false,
    situacao: 'CLASSIFICADA',
    motivo: null,
    opcaoId: 0,
    fornecedor: { ...FORNECEDOR_ALFA, tipo: 'FABRICANTE', ativo: true },
    produtoAtivo: true,
    condicaoPagamento: null,
    prazoPagamentoBase: null,
    quantidade: 2,
    statusCalculo: 'CALCULADO',
    valorOperacao: 325,
    totalCreditos: 75.89,
    custoEfetivo: 249.11,
    calculo: calculoTeste(),
    ...extra,
  };
}

/** Classificadas na ordem do backend: 1º e 2º empatados, depois o 3º. */
export const ALTERNATIVAS_TESTE: Alternativa[] = [
  alternativaTeste({
    posicao: 1,
    empate: true,
    opcaoId: 201,
    condicaoPagamento: '30 dias',
    prazoPagamentoBase: '28/56/84 dias',
  }),
  alternativaTeste({
    posicao: 2,
    empate: true,
    opcaoId: 202,
    fornecedor: { ...FORNECEDOR_BETA, tipo: 'ATACADISTA', ativo: true },
    calculo: calculoTeste({ id: 502 }),
  }),
  alternativaTeste({
    posicao: 3,
    opcaoId: 203,
    fornecedor: { ...FORNECEDOR_GAMA, tipo: 'ATACADISTA', ativo: true },
    condicaoPagamento: '60 dias',
    valorOperacao: 330,
    totalCreditos: 50.5,
    custoEfetivo: 279.5,
    calculo: calculoTeste({ id: 503, custoEfetivo: 279.5 }),
  }),
];

function naoClassificada(
  opcaoId: number,
  situacao: SituacaoAlternativa,
  motivo: string,
  extra: Partial<Alternativa> = {},
): Alternativa {
  return alternativaTeste({
    opcaoId,
    situacao,
    motivo,
    fornecedor: {
      id: opcaoId,
      razaoSocial: `Fornecedor ${String(opcaoId)} Ficticio`,
      tipo: 'FABRICANTE',
      ativo: true,
    },
    ...extra,
  });
}

export const NAO_CLASSIFICADAS_TESTE: Alternativa[] = [
  naoClassificada(
    301,
    'CALCULO_INCOMPLETO',
    'O custo efetivo não pôde ser calculado: Nenhuma regra ativa se aplica ao ICMS (teste).',
    {
      statusCalculo: 'INCOMPLETO',
      totalCreditos: null,
      custoEfetivo: null,
      calculo: CALCULO_INCOMPLETO,
    },
  ),
  naoClassificada(302, 'FORNECEDOR_DESATIVADO', 'O fornecedor está desativado (teste).', {
    fornecedor: {
      id: 302,
      razaoSocial: 'Fornecedor 302 Ficticio',
      tipo: 'ATACADISTA',
      ativo: false,
    },
    statusCalculo: null,
    valorOperacao: null,
    totalCreditos: null,
    custoEfetivo: null,
    calculo: null,
  }),
  naoClassificada(303, 'PRODUTO_DESATIVADO', 'O produto está desativado (teste).', {
    produtoAtivo: false,
    statusCalculo: null,
    valorOperacao: null,
    totalCreditos: null,
    custoEfetivo: null,
    calculo: null,
  }),
  naoClassificada(
    304,
    'CFOP_NAO_PARTICIPANTE',
    'O CFOP 6910 da operação não está em CFOPS_PARTICIPANTES (teste).',
  ),
  naoClassificada(
    305,
    'CFOPS_PARTICIPANTES_NAO_DEFINIDOS',
    'A operação tem o CFOP 6102, mas CFOPS_PARTICIPANTES não foi definido (teste).',
  ),
  naoClassificada(
    306,
    'QUANTIDADE_DIVERGENTE',
    'A quantidade da operação calculada (5) difere da quantidade da cotação (2) (teste).',
    { quantidade: 5, valorOperacao: 500, totalCreditos: 100, custoEfetivo: 400 },
  ),
];

export const COMPARACAO_TESTE: Comparacao = {
  id: 7,
  cotacaoId: 3,
  executadoEm: '2026-09-20T15:00:00Z',
  executadoPorId: 2,
  produtoId: PRODUTO_TESTE.id,
  produtoNome: PRODUTO_TESTE.nome,
  quantidade: 2,
  criterioOrdenacao: 'Custo efetivo crescente. Empate: ordem de cadastro da opção na cotação.',
  totalOpcoes: 9,
  totalClassificadas: 3,
  configuracao: {
    parametros: [
      { chave: 'UF_DESTINO', valor: 'GO' },
      { chave: 'FONTE_VALORES_OPERACAO', valor: 'INFORMADO' },
      { chave: 'CFOPS_PARTICIPANTES', valor: null },
    ],
    regrasAtivas: [
      { id: 1, versao: 3, nome: 'PIS/COFINS 6,35% (teste)', tributo: 'PIS_COFINS', prioridade: 0 },
      { id: 4, versao: 2, nome: 'ICMS interestadual (teste)', tributo: 'ICMS', prioridade: 5 },
    ],
  },
  alternativas: ALTERNATIVAS_TESTE,
  naoClassificadas: NAO_CLASSIFICADAS_TESTE,
};

export const COMPARACAO_RESUMO_TESTE: ComparacaoResumo = {
  id: 7,
  cotacaoId: 3,
  executadoEm: '2026-09-20T15:00:00Z',
  executadoPorId: 2,
  totalOpcoes: 9,
  totalClassificadas: 3,
};

export const COTACAO_RESUMO_TESTE: CotacaoResumo = {
  id: 3,
  produtoId: PRODUTO_TESTE.id,
  produtoNome: PRODUTO_TESTE.nome,
  quantidade: 2,
  descricao: 'Cotação de teste',
  criadoEm: '2026-09-20T14:59:00Z',
  criadoPorId: 2,
};

function opcaoTeste(id: number, fornecedor: { id: number; razaoSocial: string }): OpcaoCotacao {
  return {
    id,
    fornecedorId: fornecedor.id,
    fornecedorRazaoSocial: fornecedor.razaoSocial,
    nfeItemId: null,
    condicaoPagamento: null,
    observacao: null,
    valores: null,
    dadosFiscais: null,
    criadoEm: '2026-09-20T14:59:00Z',
  };
}

export const COTACAO_TESTE: Cotacao = {
  id: 3,
  produto: { id: PRODUTO_TESTE.id, nome: PRODUTO_TESTE.nome, ativo: true },
  quantidade: 2,
  descricao: 'Cotação de teste',
  criadoEm: '2026-09-20T14:59:00Z',
  criadoPorId: 2,
  opcoes: [
    {
      ...opcaoTeste(201, FORNECEDOR_ALFA),
      condicaoPagamento: '30 dias',
      valores: {
        valorProduto: 325,
        valorIpi: null,
        valorFrete: 0,
        valorSeguro: null,
        valorOutrasDespesas: null,
        valorDesconto: null,
      },
      dadosFiscais: {
        origemMercadoria: '0',
        cfop: null,
        aliquotaIcms: 4,
        aliquotaIpi: 15,
        aliquotaPis: null,
        aliquotaCofins: null,
      },
    },
    opcaoTeste(202, FORNECEDOR_BETA),
    opcaoTeste(203, FORNECEDOR_GAMA),
    opcaoTeste(301, { id: 301, razaoSocial: 'Fornecedor 301 Ficticio' }),
  ],
  todasAsOpcoesComparadas: true,
  ultimaComparacao: COMPARACAO_TESTE,
};
