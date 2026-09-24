import { FileInput, Text } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CHAVE_NFES, nfeApi } from '../../api/nfe';
import { FormularioModal } from '../../components/FormularioModal';
import type { Nfe } from '../../types/nfe';
import { formatarTamanhoArquivo, validarArquivoXml } from './validacaoNfe';

interface ImportacaoNfeProps {
  aoImportar: (nfe: Nfe) => void;
  aoCancelar: () => void;
}

/**
 * Upload manual do XML de NF-e (RF05, somente ADMIN). Sem consulta à SEFAZ nem certificado:
 * o arquivo é enviado ao backend, que valida, processa e grava a nota.
 */
export function ImportacaoNfe({ aoImportar, aoCancelar }: ImportacaoNfeProps) {
  const queryClient = useQueryClient();
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [erroArquivo, setErroArquivo] = useState<string | undefined>();

  const importar = useMutation({
    mutationFn: (selecionado: File) => nfeApi.importar(selecionado),
    onSuccess: async (nfe) => {
      await queryClient.invalidateQueries({ queryKey: CHAVE_NFES });
      aoImportar(nfe);
    },
  });

  const enviar = () => {
    if (importar.isPending) {
      return;
    }
    const problema = validarArquivoXml(arquivo);
    setErroArquivo(problema);
    if (!problema && arquivo) {
      importar.mutate(arquivo);
    }
  };

  return (
    <FormularioModal
      titulo="Importar NF-e"
      rotuloSalvar="Importar"
      salvando={importar.isPending}
      erro={importar.error}
      campos={['arquivo']}
      aoEnviar={enviar}
      aoCancelar={aoCancelar}
    >
      <Text size="sm" c="dimmed">
        Selecione o XML de uma NF-e (modelo 55). O emitente precisa estar cadastrado como fornecedor
        ativo. Os itens são vinculados aos produtos pelo GTIN/EAN.
      </Text>
      <FileInput
        label="Arquivo XML"
        placeholder="Selecionar arquivo"
        description="Até 1 MB. Para trocar o arquivo, selecione outro ou limpe a seleção."
        accept=".xml,application/xml,text/xml"
        required
        clearable
        clearButtonProps={{ 'aria-label': 'Remover arquivo selecionado' }}
        disabled={importar.isPending}
        data-autofocus
        value={arquivo}
        error={erroArquivo}
        onChange={(selecionado) => {
          setArquivo(selecionado);
          setErroArquivo(undefined);
          importar.reset();
        }}
      />
      {arquivo && (
        <Text size="sm" aria-live="polite">
          Arquivo selecionado: <strong>{arquivo.name}</strong> (
          {formatarTamanhoArquivo(arquivo.size)})
        </Text>
      )}
      {importar.isPending && (
        <Text size="sm" c="dimmed" role="status">
          Enviando e processando o XML…
        </Text>
      )}
    </FormularioModal>
  );
}
