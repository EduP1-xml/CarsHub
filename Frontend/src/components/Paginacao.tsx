import type { PaginaRespostaDTO } from '../types/api'
import { Botao } from './Botao'

interface PaginacaoProps<T> {
  envelope: PaginaRespostaDTO<T>
  aoMudarPagina: (pagina: number) => void
}

/**
 * Controles "anterior/próxima" usando pagina/ultima (contrato da seção 3 do guia).
 * "Próxima" desabilitada quando ultima === true.
 */
export function Paginacao<T>({ envelope, aoMudarPagina }: PaginacaoProps<T>) {
  const { pagina, totalElementos, totalPaginas, ultima } = envelope

  if (totalElementos === 0) return null

  return (
    <div className="paginacao">
      <Botao
        variante="secundario"
        disabled={pagina <= 0}
        onClick={() => aoMudarPagina(pagina - 1)}
        aria-label="Página anterior"
      >
        ← Anterior
      </Botao>
      <span className="paginacao-info">
        Página <strong>{pagina + 1}</strong> de <strong>{Math.max(totalPaginas, 1)}</strong> ·{' '}
        {totalElementos} registro(s)
      </span>
      <Botao
        variante="secundario"
        disabled={ultima}
        onClick={() => aoMudarPagina(pagina + 1)}
        aria-label="Próxima página"
      >
        Próxima →
      </Botao>
    </div>
  )
}
