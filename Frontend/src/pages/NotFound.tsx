import { Link } from 'react-router-dom'

/** 404 de rota do frontend (mensagem local — não confundir com o 404 do backend). */
export function NotFound() {
  return (
    <div className="estado-vazio">
      <div className="estado-vazio-icone" aria-hidden="true">
        🛣️
      </div>
      <h3>Página não encontrada</h3>
      <p>A rota que você acessou não existe neste aplicativo.</p>
      <div className="estado-vazio-acao">
        <Link to="/eventos" className="botao botao-primario" style={{ textDecoration: 'none' }}>
          Ir para o catálogo de eventos
        </Link>
      </div>
    </div>
  )
}
