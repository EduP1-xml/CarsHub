// Hook de formulário leve: valores + mapa de erros por campo (400 = MAPA por campo — R13).
import { useCallback, useState } from 'react'

export interface UseFormResult<T> {
  valores: T
  setValor: <K extends keyof T>(campo: K, valor: T[K]) => void
  setValores: (parciais: Partial<T>) => void
  reset: (novos: T) => void
  /** Mapa {campo: mensagem} vindo do backend (400 Bean Validation). */
  errosCampos: Record<string, string>
  setErrosCampos: (mapa: Record<string, string>) => void
  limparErros: () => void
}

export function useForm<T extends object>(inicial: T): UseFormResult<T> {
  const [valores, setValoresEstado] = useState<T>(inicial)
  const [errosCampos, setErrosCamposEstado] = useState<Record<string, string>>({})

  const setValor = useCallback(<K extends keyof T>(campo: K, valor: T[K]) => {
    setValoresEstado((atuais) => ({ ...atuais, [campo]: valor }) as T)
    setErrosCamposEstado((atuais) => {
      if (!(campo as string in atuais)) return atuais
      const copia = { ...atuais }
      delete copia[campo as string]
      return copia
    })
  }, [])

  const setValores = useCallback((parciais: Partial<T>) => {
    setValoresEstado((atuais) => ({ ...atuais, ...parciais }))
  }, [])

  const reset = useCallback((novos: T) => {
    setValoresEstado(novos)
    setErrosCamposEstado({})
  }, [])

  const setErrosCampos = useCallback((mapa: Record<string, string>) => setErrosCamposEstado(mapa), [])
  const limparErros = useCallback(() => setErrosCamposEstado({}), [])

  return { valores, setValor, setValores, reset, errosCampos, setErrosCampos, limparErros }
}
