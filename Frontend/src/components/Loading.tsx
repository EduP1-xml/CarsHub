interface LoadingProps {
  rotulo?: string
}

export function Loading({ rotulo = 'Carregando...' }: LoadingProps) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="loading-spinner" aria-hidden="true" />
      <span>{rotulo}</span>
    </div>
  )
}

export function LoadingPagina({ rotulo = 'Carregando...' }: LoadingProps) {
  return (
    <div className="loading-pagina">
      <Loading rotulo={rotulo} />
    </div>
  )
}
