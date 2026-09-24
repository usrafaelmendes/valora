import { Anchor, Button, Stack, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router';
import { CHAVE_NFES, nfeApi } from '../../api/nfe';
import { AvisoSucesso } from '../../components/AvisoSucesso';
import { CabecalhoPagina } from '../../components/CabecalhoPagina';
import { EstadoVazio } from '../../components/EstadoVazio';
import { ResultadoConsulta } from '../../components/ResultadoConsulta';
import { NaoInformado, TabelaDados, type ColunaTabela } from '../../components/TabelaDados';
import { caminhoDetalheNfe } from '../../routes/rotas';
import type { Nfe, NfeResumo } from '../../types/nfe';
import { formatarDataHora, formatarMoeda } from '../../utils/formatacao';
import { ImportacaoNfe } from './ImportacaoNfe';

/** NF-e importadas (RF05 a RF07, RF14). Área exclusiva do ADMIN, como no backend. */
export function NfePage() {
  const [importando, setImportando] = useState(false);
  const [importada, setImportada] = useState<Nfe | null>(null);

  const consulta = useQuery({
    queryKey: CHAVE_NFES,
    queryFn: ({ signal }) => nfeApi.listar(signal),
  });

  const abrirImportacao = () => {
    setImportada(null);
    setImportando(true);
  };

  const colunas: ColunaTabela<NfeResumo>[] = [
    {
      titulo: 'Número/série',
      semQuebra: true,
      conteudo: (nfe) => (
        <Anchor component={Link} to={caminhoDetalheNfe(nfe.id)} fw={500}>
          {`Nº ${String(nfe.numero)} / ${String(nfe.serie)}`}
        </Anchor>
      ),
    },
    { titulo: 'Emissão', semQuebra: true, conteudo: (nfe) => formatarDataHora(nfe.dataEmissao) },
    { titulo: 'Fornecedor', conteudo: (nfe) => nfe.fornecedorRazaoSocial },
    { titulo: 'Natureza da operação', conteudo: (nfe) => nfe.naturezaOperacao },
    {
      titulo: 'Valor total',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (nfe) =>
        nfe.valorTotal === null ? <NaoInformado /> : formatarMoeda(nfe.valorTotal),
    },
    {
      titulo: 'Importada em',
      semQuebra: true,
      conteudo: (nfe) => formatarDataHora(nfe.importadoEm),
    },
    {
      titulo: 'Ações',
      alinhamento: 'right',
      semQuebra: true,
      conteudo: (nfe) => (
        <Button
          component={Link}
          to={caminhoDetalheNfe(nfe.id)}
          size="xs"
          variant="default"
          aria-label={`Ver detalhes da NF-e ${String(nfe.numero)}`}
        >
          Detalhes
        </Button>
      ),
    },
  ];

  return (
    <>
      <CabecalhoPagina
        titulo="NF-e"
        descricao="Notas importadas manualmente a partir do XML. Os dados fiscais são os originais da nota e servem de histórico para os cálculos."
        acoes={<Button onClick={abrirImportacao}>Importar NF-e</Button>}
      />

      <Stack gap="md">
        {importada && (
          <AvisoSucesso
            mensagem={
              <>
                {`NF-e nº ${String(importada.numero)} importada com ${String(importada.itens.length)} ${
                  importada.itens.length === 1 ? 'item' : 'itens'
                }`}
                {importada.itensSemProduto > 0 &&
                  ` (${String(importada.itensSemProduto)} sem produto vinculado)`}
                {'. '}
                <Anchor component={Link} to={caminhoDetalheNfe(importada.id)} size="sm">
                  Ver detalhes
                </Anchor>
              </>
            }
            aoFechar={() => {
              setImportada(null);
            }}
          />
        )}

        <ResultadoConsulta
          consulta={consulta}
          mensagemCarregando="Carregando NF-e…"
          tituloErro="Não foi possível carregar as NF-e"
          vazio={
            <EstadoVazio
              titulo="Nenhuma NF-e importada"
              descricao="Importe o XML de uma NF-e de um fornecedor cadastrado para registrar seus dados."
              acao={<Button onClick={abrirImportacao}>Importar NF-e</Button>}
            />
          }
        >
          {(nfes) => (
            <TabelaDados
              rotulo="NF-e importadas"
              itens={nfes}
              colunas={colunas}
              obterChave={(nfe) => nfe.id}
              larguraMinima={900}
            />
          )}
        </ResultadoConsulta>

        <Text c="dimmed" size="xs">
          A importação é manual: o sistema não consulta a SEFAZ nem usa certificado digital.
        </Text>
      </Stack>

      {importando && (
        <ImportacaoNfe
          aoCancelar={() => {
            setImportando(false);
          }}
          aoImportar={(nfe) => {
            setImportando(false);
            setImportada(nfe);
          }}
        />
      )}
    </>
  );
}
