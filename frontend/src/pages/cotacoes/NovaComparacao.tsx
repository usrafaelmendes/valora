import { Button, Text } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { chaveComparacao } from '../../api/comparacoes';
import { CHAVE_COTACOES, cotacoesApi } from '../../api/cotacoes';
import { ConfirmacaoAcao } from '../../components/ConfirmacaoAcao';
import { caminhoComparacao, type EstadoAviso } from '../../routes/rotas';

interface NovaComparacaoProps {
  cotacaoId: number;
  variante?: 'filled' | 'default';
}

/**
 * Executa uma nova comparação da cotação (POST /cotacoes/{id}/comparacoes) após confirmação.
 * O backend cria um novo registro com a configuração vigente; as comparações anteriores
 * continuam no histórico, sem alteração. Após o sucesso, abre a nova comparação.
 */
export function NovaComparacao({ cotacaoId, variante = 'filled' }: NovaComparacaoProps) {
  const [confirmando, setConfirmando] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const comparar = useMutation({
    mutationFn: () => cotacoesApi.comparar(cotacaoId),
    onSuccess: async (comparacao) => {
      queryClient.setQueryData(chaveComparacao(comparacao.id), comparacao);
      await queryClient.invalidateQueries({ queryKey: CHAVE_COTACOES });
      setConfirmando(false);
      const estado: EstadoAviso = {
        aviso: `Nova comparação nº ${String(comparacao.id)} executada com a configuração vigente.`,
      };
      void navigate(caminhoComparacao(comparacao.id), { state: estado });
    },
  });

  return (
    <>
      <Button
        variant={variante}
        onClick={() => {
          comparar.reset();
          setConfirmando(true);
        }}
      >
        Nova comparação
      </Button>
      {confirmando && (
        <ConfirmacaoAcao
          titulo="Executar nova comparação"
          rotuloConfirmar="Executar comparação"
          corConfirmar="blue"
          processando={comparar.isPending}
          erro={comparar.error}
          aoConfirmar={() => {
            comparar.mutate();
          }}
          aoCancelar={() => {
            setConfirmando(false);
          }}
        >
          <Text size="sm">
            {`Uma nova comparação da cotação nº ${String(cotacaoId)} será criada com todas as opções e com as regras e os parâmetros vigentes agora.`}
          </Text>
          <Text size="sm" mt="xs" c="dimmed">
            As comparações anteriores continuam no histórico, sem alteração.
          </Text>
        </ConfirmacaoAcao>
      )}
    </>
  );
}
