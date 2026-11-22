// Toasts leves — exibem as mensagens do backend SEM traduzir (regra de ouro).
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'

type TipoToast = 'sucesso' | 'erro' | 'info'

interface Toast {
  id: number
  tipo: TipoToast
  mensagem: string
}

interface ToastContextValue {
  toast: (tipo: TipoToast, mensagem: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const contador = useRef(0)

  const toast = useCallback((tipo: TipoToast, mensagem: string) => {
    const id = ++contador.current
    setToasts((atuais) => [...atuais, { id, tipo, mensagem }])
    window.setTimeout(() => {
      setToasts((atuais) => atuais.filter((t) => t.id !== id))
    }, 7000)
  }, [])

  const valor = useMemo(() => ({ toast }), [toast])

  return (
    <ToastContext.Provider value={valor}>
      {children}
      <div className="toast-container" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.tipo}`} role="status">
            <span className="toast-texto">{t.mensagem}</span>
            <button
              type="button"
              className="toast-fechar"
              aria-label="Fechar aviso"
              onClick={() => setToasts((atuais) => atuais.filter((x) => x.id !== t.id))}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const contexto = useContext(ToastContext)
  if (!contexto) throw new Error('useToast deve ser usado dentro de <ToastProvider>')
  return contexto
}
