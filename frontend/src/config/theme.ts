import { createTheme, type MantineColorsTuple } from '@mantine/core';

/**
 * Identidade visual: azul-petróleo sóbrio sobre fundo neutro, tipografia do sistema
 * operacional e números tabulares, priorizando leitura de tabelas e formulários.
 */
const petroleo: MantineColorsTuple = [
  '#e8f4f6',
  '#d3e6ea',
  '#a5ccd4',
  '#74b1bd',
  '#4c9aa9',
  '#33899b',
  '#257f93',
  '#146d80',
  '#036173',
  '#0b3a45',
];

export const theme = createTheme({
  colors: { petroleo },
  primaryColor: 'petroleo',
  primaryShade: 7,
  fontFamily:
    'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif',
  headings: { fontWeight: '650' },
  defaultRadius: 'sm',
  cursorType: 'pointer',
  components: {
    Table: {
      defaultProps: { highlightOnHover: true, verticalSpacing: 'sm' },
    },
    Paper: {
      defaultProps: { shadow: 'none' },
    },
    // Em colunas estreitas de tabela o Badge encolhia e cortava o texto ("INATI…");
    // a situação precisa ficar legível, então ele ocupa sempre a largura do próprio texto.
    Badge: {
      styles: { root: { flexShrink: 0, minWidth: 'max-content' } },
    },
  },
});
