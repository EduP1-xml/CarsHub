import type { ReactNode } from 'react'
import { Botao } from './Botao'

interface ModalProps {
  aberto: boolean
  titulo: string
  children: ReactNode
  /** Rótulo do botão de confirmação (null = sem botão). */
  confirmarRotulo?: string
  varianteConfirmar?: 'primario' | 'perigo' | 'sucesso'
  confirmarCarregando?: boolean
  aoConfirmar?: () => void
  aoFechar: () => void
  cancelarRotulo?: string
}

export function Modal({
  aberto,
  titulo,
  children,
  confirmarRotulo,
  varianteConfirmar = 'primario',
  confirmarCarregando = false,
  aoConfirmar,
  aoFechar,
  cancelarRotulo = 'Cancelar',
}: ModalProps) {
  if (!aberto) return null
  return (
    <div className="modal-fundo" role="dialog" aria-modal="true" aria-label={titulo}>
      <div className="modal-caixa">
        <div className="modal-cabecalho">
          <h3>{titulo}</h3>
          <button type="button" className="modal-fechar" aria-label="Fechar" onClick={aoFechar}>
            ×
          </button>
        </div>
        <div className="modal-corpo">{children}</div>
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoFechar}>
            {cancelarRotulo}
          </Botao>
          {confirmarRotulo && aoConfirmar && (
            <Botao variante={varianteConfirmar} carregando={confirmarCarregando} onClick={aoConfirmar}>
              {confirmarRotulo}
            </Botao>
          )}
        </div>
      </div>
    </div>
  )
}
