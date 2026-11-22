import type { ReactNode } from 'react'

interface EstadoVazioProps {
  titulo: string
  descricao?: string
  acao?: ReactNode
}

export function EstadoVazio({ titulo, descricao, acao }: EstadoVazioProps) {
  return (
    <div className="estado-vazio">
      <div className="estado-vazio-icone" aria-hidden="true">
        🚗
      </div>
      <h3>{titulo}</h3>
      {descricao && <p>{descricao}</p>}
      {acao && <div className="estado-vazio-acao">{acao}</div>}
    </div>
  )
}
