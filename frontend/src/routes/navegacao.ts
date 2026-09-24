import type { Perfil } from '../types/auth';
import { ROTAS } from './rotas';

export interface ItemNavegacao {
  rotulo: string;
  caminho: string;
}

export interface SecaoNavegacao {
  titulo: string;
  itens: ItemNavegacao[];
  /** Perfis que veem a seção. O backend continua sendo quem autoriza cada operação. */
  perfis: Perfil[];
}

/**
 * Menu lateral por perfil, seguindo REQUISITOS §3 e as permissões do backend: o USER
 * consulta produtos e fornecedores (GET liberado a qualquer autenticado) e realiza
 * cotações; NF-e, configurações do cálculo e usuários são administrativos.
 */
export const NAVEGACAO: SecaoNavegacao[] = [
  {
    titulo: 'Geral',
    perfis: ['ADMIN', 'USER'],
    itens: [
      { rotulo: 'Início', caminho: ROTAS.inicio },
      { rotulo: 'Cotações', caminho: ROTAS.cotacoes },
      { rotulo: 'Produtos', caminho: ROTAS.produtos },
      { rotulo: 'Fornecedores', caminho: ROTAS.fornecedores },
    ],
  },
  {
    titulo: 'Administração',
    perfis: ['ADMIN'],
    itens: [
      { rotulo: 'NF-e', caminho: ROTAS.nfe },
      { rotulo: 'Regras tributárias', caminho: ROTAS.regrasTributarias },
      { rotulo: 'Parâmetros de cálculo', caminho: ROTAS.parametrosCalculo },
      { rotulo: 'Usuários', caminho: ROTAS.usuarios },
    ],
  },
];

export function navegacaoDoPerfil(perfil: Perfil): SecaoNavegacao[] {
  return NAVEGACAO.filter((secao) => secao.perfis.includes(perfil));
}
