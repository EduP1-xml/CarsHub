// Hook de paginação — contrato ÚNICO do envelope PaginaRespostaDTO (seção 3 do guia).
// pagina é base 0; botões anterior/próxima usam pagina/ultima.
import { useCallback, useEffect, useState } from 'react'
import { ENVELOPE_VAZIO, type PaginaRespostaDTO } from '../types/api'
import { extrairErro, type ErroNormalizado } from '../utils/erros'

export type Buscador<T> = (pagina: number, tamanho: number) => Promise<{ data: PaginaRespostaDTO<T> }>

export interface EstadoPaginacao<T> {
  envelope: PaginaRespostaDTO<T>
  carregando: boolean
  erro: ErroNormalizado | null
  pagina: number
  tamanho: number
  recarregar: () => void
  irParaPagina: (pagina: number) => void
  /** refetch da página atual (depois de uma ação que muda os dados). */
  atualizar: () => void
}

export function usePaginacao<T>(buscador: Buscador<T>, tamanho = 5, deps: unknown[] = []): EstadoPaginacao<T> {
  const [envelope, setEnvelope] = useState<PaginaRespostaDTO<T>>(ENVELOPE_VAZIO)
  const [pagina, setPagina] = useState(0)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<ErroNormalizado | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    // Mudança nos filtros/contexto → volta para a página 0.
    setPagina(0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    let cancelado = false
    setCarregando(true)
    setErro(null)
    buscador(pagina, tamanho)
      .then((resposta) => {
        if (!cancelado) setEnvelope(resposta.data)
      })
      .catch((e) => {
        if (!cancelado) setErro(extrairErro(e))
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, tamanho, versao, ...deps])

  const recarregar = useCallback(() => setVersao((v) => v + 1), [])
  const atualizar = useCallback(() => setVersao((v) => v + 1), [])
  const irParaPagina = useCallback((p: number) => setPagina(Math.max(0, p)), [])

  return { envelope, carregando, erro, pagina, tamanho, recarregar, irParaPagina, atualizar }
}
