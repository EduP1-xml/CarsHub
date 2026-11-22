// Contador regressivo para cooldowns espelhados na UI (reenvio 60 s, solicitar código 60 s,
// e o Retry-After do 429). A regra final é SEMPRE do backend — aqui só desabilitamos o botão.
import { useEffect, useRef, useState } from 'react'

export interface CountdownResult {
  restante: number
  ativo: boolean
  iniciar: (segundos: number) => void
}

export function useCountdown(): CountdownResult {
  const [restante, setRestante] = useState(0)
  const temporizador = useRef<number | null>(null)

  useEffect(() => {
    return () => {
      if (temporizador.current !== null) window.clearInterval(temporizador.current)
    }
  }, [])

  function iniciar(segundos: number) {
    if (segundos <= 0) return
    if (temporizador.current !== null) window.clearInterval(temporizador.current)
    setRestante(segundos)
    temporizador.current = window.setInterval(() => {
      setRestante((atual) => {
        if (atual <= 1) {
          if (temporizador.current !== null) window.clearInterval(temporizador.current)
          return 0
        }
        return atual - 1
      })
    }, 1000)
  }

  return { restante, ativo: restante > 0, iniciar }
}
