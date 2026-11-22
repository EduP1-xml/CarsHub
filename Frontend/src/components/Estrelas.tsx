interface EstrelasProps {
  /** Nota atual (1..5). */
  nota: number
  /** Quando informado, permite escolher; sem ele, exibe somente leitura. */
  aoEscolher?: (nota: number) => void
  rotulo?: string
}

/** Avaliação 1..5 (estrelas) — envio e exibição. */
export function Estrelas({ nota, aoEscolher, rotulo }: EstrelasProps) {
  const interativa = !!aoEscolher
  return (
    <div className="estrelas" role={interativa ? 'radiogroup' : undefined} aria-label={rotulo ?? 'Nota'}>
      {[1, 2, 3, 4, 5].map((valor) => (
        <button
          key={valor}
          type="button"
          className={`estrela${valor <= nota ? ' estrela-ativa' : ''}`}
          disabled={!interativa}
          aria-label={`Nota ${valor}`}
          onClick={() => aoEscolher?.(valor)}
        >
          ★
        </button>
      ))}
    </div>
  )
}
