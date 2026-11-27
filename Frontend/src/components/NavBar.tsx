import { NavLink, useNavigate } from 'react-router-dom'
import { ehColecionador, ehGuincho, ehMecanico, ehOrganizador, useAuth } from '../context/AuthContext'
import { LABEL_TIPO_PRESTADOR, LABEL_TIPO_USUARIO } from '../utils/enums'

function classeLink({ isActive }: { isActive: boolean }): string {
  return isActive ? 'nav-link nav-link-ativo' : 'nav-link'
}

export function NavBar() {
  const { usuario, logout } = useAuth()
  const navigate = useNavigate()

  const colecionador = ehColecionador(usuario)
  const organizador = ehOrganizador(usuario)
  const mecanico = ehMecanico(usuario)
  const guincho = ehGuincho(usuario)
  const entusiasta = usuario?.tipoUsuario === 'ENTUSIASTA' || colecionador

  function sair() {
    logout()
    navigate('/login')
  }

  // Destino da marca: convidado → catálogo público; logado → home do próprio perfil.
  const destinoMarca = !usuario ? '/eventos' : organizador ? '/dashboard' : entusiasta ? '/eventos' : '/disponiveis'

  return (
    <header className="navbar">
      <div className="navbar-conteudo">
        <NavLink to={destinoMarca} className="navbar-marca">
          🚗 CarsHub
        </NavLink>

        <nav className="navbar-links" aria-label="Navegação principal">
          <NavLink to="/eventos" className={classeLink}>
            Eventos
          </NavLink>
          {!usuario && (
            <>
              <NavLink to="/login" className={classeLink}>
                Entrar
              </NavLink>
              <NavLink to="/cadastro" className={classeLink}>
                Criar conta
              </NavLink>
            </>
          )}
          {entusiasta && (
            <>
              <NavLink to="/garagem" className={classeLink}>
                Minha garagem
              </NavLink>
              <NavLink to="/inscricoes" className={classeLink}>
                Minhas inscrições
              </NavLink>
              <NavLink to="/clubes" className={classeLink}>
                Clubes
              </NavLink>
            </>
          )}
          {colecionador && (
            <NavLink to="/servicos" className={classeLink}>
              Meus serviços
            </NavLink>
          )}
          {organizador && (
            <>
              <NavLink to="/meus-eventos" className={classeLink}>
                Meus eventos
              </NavLink>
              <NavLink to="/dashboard" className={classeLink}>
                Dashboard
              </NavLink>
            </>
          )}
          {mecanico && (
            <>
              <NavLink to="/disponiveis" className={classeLink}>
                Chamados disponíveis
              </NavLink>
              <NavLink to="/meus-servicos" className={classeLink}>
                Minha fila
              </NavLink>
            </>
          )}
          {guincho && (
            <>
              <NavLink to="/disponiveis" className={classeLink}>
                Chamados disponíveis
              </NavLink>
              <NavLink to="/meus-servicos" className={classeLink}>
                Minha fila
              </NavLink>
            </>
          )}
        </nav>

        {usuario && (
          <div className="navbar-usuario">
            <NavLink to="/perfil" className="navbar-perfil" title="Meu perfil">
              {usuario.urlFotoPerfil ? (
                <img className="navbar-avatar" src={usuario.urlFotoPerfil} alt="Foto de perfil" />
              ) : (
                <span className="navbar-avatar navbar-avatar-vazio">👤</span>
              )}
              <span className="navbar-nome">{usuario.nomeUsuario}</span>
              <span className="navbar-tipo">
                {mecanico || guincho
                  ? LABEL_TIPO_PRESTADOR[usuario.tipoPrestadorServico!]
                  : LABEL_TIPO_USUARIO[usuario.tipoUsuario]}
              </span>
            </NavLink>
            <button type="button" className="botao botao-texto navbar-sair" onClick={sair}>
              Sair
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
