import type { ButtonHTMLAttributes, ReactNode } from 'react'

interface BotaoProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: 'primario' | 'secundario' | 'perigo' | 'sucesso' | 'texto'
  carregando?: boolean
  children: ReactNode
}

export function Botao({ variante = 'primario', carregando = false, disabled, children, ...resto }: BotaoProps) {
  return (
    <button
      type="button"
      className={`botao botao-${variante}`}
      disabled={disabled || carregando}
      {...resto}
    >
      {carregando && <span className="botao-spinner" aria-hidden="true" />}
      {children}
    </button>
  )
}
