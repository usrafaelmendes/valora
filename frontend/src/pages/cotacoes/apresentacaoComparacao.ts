import type { SituacaoAlternativa, StatusCalculo } from '../../types/cotacao';
import { ROTULO_FORMA_ALIQUOTA, ROTULO_TRIBUTO } from '../../types/regraTributaria';

/**
 * Apresentação dos códigos devolvidos pelo backend na comparação. Somente rótulos, cores e
 * explicações curtas (tiradas da documentação dos enums): a situação, o motivo, a posição e
 * os valores são sempre os gravados pelo backend. Códigos desconhecidos aparecem como vieram.
 */

interface ApresentacaoSituacao {
  rotulo: string;
  cor: string;
  /** Explicação geral da situação; o motivo específico da opção vem do backend. */
  explicacao: string;
}

export const APRESENTACAO_SITUACAO: Record<SituacaoAlternativa, ApresentacaoSituacao> = {
  CLASSIFICADA: {
    rotulo: 'Classificada',
    cor: 'teal',
    explicacao: 'Custo efetivo calculado e participação confirmada: tem posição no ranking.',
  },
  CALCULO_INCOMPLETO: {
    rotulo: 'Cálculo incompleto',
    cor: 'orange',
    explicacao:
      'O cálculo tem pendência bloqueante (parâmetro, regra, dado ausente ou conflito): o custo efetivo não foi obtido.',
  },
  FORNECEDOR_DESATIVADO: {
    rotulo: 'Fornecedor desativado',
    cor: 'gray',
    explicacao: 'O fornecedor está desativado: a opção não foi calculada nem classificada.',
  },
  PRODUTO_DESATIVADO: {
    rotulo: 'Produto desativado',
    cor: 'gray',
    explicacao: 'O produto está desativado: a opção não foi calculada nem classificada.',
  },
  CFOP_NAO_PARTICIPANTE: {
    rotulo: 'CFOP não participante',
    cor: 'grape',
    explicacao: 'O CFOP da operação não está entre os CFOPs participantes configurados.',
  },
  CFOPS_PARTICIPANTES_NAO_DEFINIDOS: {
    rotulo: 'CFOPs participantes não definidos',
    cor: 'yellow',
    explicacao:
      'A operação tem CFOP, mas os CFOPs participantes ainda não foram configurados: a participação não pode ser confirmada.',
  },
  QUANTIDADE_DIVERGENTE: {
    rotulo: 'Quantidade divergente',
    cor: 'red',
    explicacao:
      'A quantidade da operação calculada difere da quantidade da cotação: o custo não é comparável e fica fora do ranking.',
  },
};

export function apresentacaoSituacao(situacao: string): ApresentacaoSituacao {
  return (
    (APRESENTACAO_SITUACAO as Record<string, ApresentacaoSituacao | undefined>)[situacao] ?? {
      rotulo: situacao,
      cor: 'gray',
      explicacao: '',
    }
  );
}

export const ROTULO_STATUS_CALCULO: Record<StatusCalculo, string> = {
  CALCULADO: 'Calculado',
  INCOMPLETO: 'Incompleto',
};

export function rotuloStatusCalculo(status: string | null): string | null {
  if (status === null) {
    return null;
  }
  return (ROTULO_STATUS_CALCULO as Record<string, string | undefined>)[status] ?? status;
}

export function rotuloTributo(tributo: string): string {
  return (ROTULO_TRIBUTO as Record<string, string | undefined>)[tributo] ?? tributo;
}

export function rotuloFormaAliquota(forma: string | null): string | null {
  if (forma === null) {
    return null;
  }
  return (ROTULO_FORMA_ALIQUOTA as Record<string, string | undefined>)[forma] ?? forma;
}

/** Situações do crédito de um tributo (enum SituacaoCredito do backend). */
const APRESENTACAO_SITUACAO_CREDITO: Record<string, { rotulo: string; cor: string }> = {
  CALCULADO: { rotulo: 'Calculado', cor: 'teal' },
  SEM_CREDITO: { rotulo: 'Sem crédito', cor: 'gray' },
  REGRA_AUSENTE: { rotulo: 'Regra ausente', cor: 'orange' },
  CONFLITO_REGRAS: { rotulo: 'Conflito de regras', cor: 'red' },
  ALIQUOTA_INDISPONIVEL: { rotulo: 'Alíquota indisponível', cor: 'orange' },
  CONFIGURACAO_INCONSISTENTE: { rotulo: 'Configuração inconsistente', cor: 'red' },
  NAO_CALCULADO: { rotulo: 'Não calculado', cor: 'orange' },
};

export function apresentacaoSituacaoCredito(situacao: string): { rotulo: string; cor: string } {
  return APRESENTACAO_SITUACAO_CREDITO[situacao] ?? { rotulo: situacao, cor: 'gray' };
}

/** Tipos de pendência do cálculo (enum TipoPendencia do backend). */
const ROTULO_PENDENCIA: Record<string, string> = {
  PARAMETRO_NAO_DEFINIDO: 'Parâmetro não definido',
  DADO_INDISPONIVEL: 'Dado indisponível',
  VALOR_INVALIDO: 'Valor inválido',
  REGRA_AUSENTE: 'Regra ausente',
  CONFLITO_REGRAS: 'Conflito de regras',
  ALIQUOTA_INDISPONIVEL: 'Alíquota indisponível',
  CONFIGURACAO_INCONSISTENTE: 'Configuração inconsistente',
  COMPONENTE_AUSENTE_NA_NFE: 'Componente ausente na NF-e',
  CFOP_NAO_PARTICIPANTE: 'CFOP não participante',
  CFOPS_PARTICIPANTES_NAO_DEFINIDOS: 'CFOPs participantes não definidos',
  FORNECEDOR_DESATIVADO: 'Fornecedor desativado',
  PRODUTO_NAO_VINCULADO: 'Produto não vinculado',
  DADOS_FISCAIS_DE_OUTRA_NFE: 'Dados fiscais de outra NF-e',
};

export function rotuloPendencia(tipo: string): string {
  return ROTULO_PENDENCIA[tipo] ?? tipo;
}

/** Posição exibida como ordinal (1 → "1º"), exatamente como gravada pelo backend. */
export function formatarPosicao(posicao: number): string {
  return `${String(posicao)}º`;
}
