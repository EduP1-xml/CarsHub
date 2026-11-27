import type { FiltroCarroDTO } from '../types/api'
import { descreverFiltro } from './filtroCarro'

interface FiltroCarroResumoProps {
  filtro: FiltroCarroDTO | null | undefined
  /** Texto quando não há nenhum critério marcado. */
  vazio?: string
}

/** Critérios do filtro em chips ("Marca: Volkswagen", "Ano: 1960 a 1985", "Exige som"). */
export function FiltroCarroResumo({ filtro, vazio = 'Aceita qualquer carro.' }: FiltroCarroResumoProps) {
  const linhas = descreverFiltro(filtro)
  if (linhas.length === 0) return <p className="abreviado">{vazio}</p>
  return (
    <div className="chips">
      {linhas.map((linha) => (
        <span key={linha} className="chip">
          {linha}
        </span>
      ))}
    </div>
  )
}
