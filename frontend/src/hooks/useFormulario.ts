import { useCallback, useState } from 'react';
import { ApiError } from '../api/ApiError';

/** Mensagem de erro por campo do formulário. */
export type ErrosFormulario<T> = Partial<Record<keyof T, string>>;

type ValoresFormulario = Record<string, string>;

/**
 * Estado simples de formulário: valores (sempre texto, como nos inputs), erros por campo,
 * validação no envio e aplicação dos erros de validação devolvidos pela API ("erros").
 *
 * A validação local só evita requisições obviamente inválidas; a validação definitiva
 * continua no backend, cujas mensagens por campo são exibidas nos mesmos campos.
 */
export function useFormulario<T extends ValoresFormulario>(
  iniciais: T,
  validar: (valores: T) => ErrosFormulario<T>,
) {
  const [valores, setValores] = useState<T>(iniciais);
  const [erros, setErros] = useState<ErrosFormulario<T>>({});

  const alterar = useCallback(<K extends keyof T>(campo: K, valor: T[K]) => {
    setValores((atuais) => ({ ...atuais, [campo]: valor }));
    // O erro do campo some assim que o usuário o corrige.
    setErros((atuais) => ({ ...atuais, [campo]: undefined }));
  }, []);

  /** Valida todos os campos e retorna se o formulário pode ser enviado. */
  const validarTudo = useCallback((): boolean => {
    const encontrados = validar(valores);
    setErros(encontrados);
    return Object.values(encontrados).every((mensagem) => !mensagem);
  }, [validar, valores]);

  /** Mostra nos campos os erros de validação devolvidos pela API, quando houver. */
  const aplicarErrosApi = useCallback(
    (erro: unknown) => {
      if (!(erro instanceof ApiError)) {
        return;
      }
      const doFormulario = Object.entries(erro.erros).filter(([campo]) => campo in valores);
      if (doFormulario.length > 0) {
        setErros((atuais) => ({ ...atuais, ...Object.fromEntries(doFormulario) }));
      }
    },
    [valores],
  );

  return { valores, erros, alterar, validarTudo, aplicarErrosApi };
}

/** Texto opcional em branco é enviado como não informado (null), como o backend o trata. */
export function textoOpcional(valor: string): string | null {
  const texto = valor.trim();
  return texto ? texto : null;
}

/** Mensagem de limite de caracteres, ou undefined quando o texto cabe. */
export function excedeLimite(valor: string, limite: number, mensagem: string): string | undefined {
  return valor.trim().length > limite ? mensagem : undefined;
}
