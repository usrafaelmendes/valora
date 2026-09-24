import { useState } from 'react';
import { useLocation } from 'react-router';
import type { EstadoAviso } from '../routes/rotas';

/**
 * Aviso de sucesso enviado pela página anterior no estado da navegação
 * (ex.: "Cotação criada" ao abrir a comparação inicial). Retorna o aviso e como fechá-lo.
 */
export function useAvisoNavegacao(): [string | null, () => void] {
  const location = useLocation();
  const estado = location.state as EstadoAviso | null;
  const [fechadoEm, setFechadoEm] = useState<string | null>(null);
  const aviso = estado?.aviso && fechadoEm !== location.key ? estado.aviso : null;
  return [
    aviso,
    () => {
      setFechadoEm(location.key);
    },
  ];
}
