import type { ReactNode } from 'react'

interface AlertaProps {
  tipo: 'erro' | 'sucesso' | 'info' | 'aviso'
  children: ReactNode
}

/** Alerta inline — exibe a mensagem do backend SEM traduzir. */
export function Alerta({ tipo, children }: AlertaProps) {
  return (
    <div className={`alerta alerta-${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
