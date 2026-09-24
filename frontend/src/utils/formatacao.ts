/**
 * Formata o CNPJ recebido sem máscara (14 caracteres, numérico ou alfanumérico) como
 * XX.XXX.XXX/XXXX-XX. Valores fora desse formato são exibidos como vieram.
 */
export function formatarCnpj(cnpj: string): string {
  const partes = /^([0-9A-Z]{2})([0-9A-Z]{3})([0-9A-Z]{3})([0-9A-Z]{4})([0-9]{2})$/.exec(cnpj);
  if (!partes) {
    return cnpj;
  }
  const [, a, b, c, d, dv] = partes;
  return `${a ?? ''}.${b ?? ''}.${c ?? ''}/${d ?? ''}-${dv ?? ''}`;
}

/** Remove a máscara do CNPJ digitado (pontos, barra, hífen e espaços) e usa letras maiúsculas. */
export function normalizarCnpj(cnpj: string): string {
  return cnpj.replace(/[.\-/\s]/g, '').toUpperCase();
}

/**
 * Formatação de exibição apenas: nenhum valor é recalculado ou arredondado para fins de negócio.
 * Até 10 casas decimais são preservadas (limite do leiaute da NF-e para valores unitários).
 */
const MAXIMO_CASAS = 10;

/** Valor em reais, com ao menos 2 casas e sem cortar casas informadas pelo backend. */
export function formatarMoeda(valor: number): string {
  return valor.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: MAXIMO_CASAS,
  });
}

/** Número decimal (ex.: quantidade, fator) com as casas informadas pelo backend. */
export function formatarNumero(valor: number): string {
  return valor.toLocaleString('pt-BR', { maximumFractionDigits: MAXIMO_CASAS });
}

/** Percentual já expresso em pontos (ex.: 7.5 → "7,5%"), como enviado pelo backend. */
export function formatarPercentual(valor: number): string {
  return `${formatarNumero(valor)}%`;
}

/** Instantes são exibidos no horário de Brasília, como na tabela exportada pelo backend. */
const FORMATO_DATA_HORA = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'America/Sao_Paulo',
});

/** Instante ISO-8601 como data e hora; valores inválidos são exibidos como vieram. */
export function formatarDataHora(instante: string): string {
  const data = new Date(instante);
  return Number.isNaN(data.getTime()) ? instante : FORMATO_DATA_HORA.format(data);
}

/** Chave de acesso da NF-e (44 dígitos) em blocos de 4, para facilitar a leitura. */
export function formatarChaveAcesso(chave: string): string {
  return /^\d{44}$/.test(chave) ? (chave.match(/\d{4}/g) ?? []).join(' ') : chave;
}
