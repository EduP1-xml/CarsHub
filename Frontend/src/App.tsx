// Definição das rotas (react-router-dom) + guardas por perfil (seção 5 do guia).
// As telas escondem o que não pertence ao perfil, mas a regra final é SEMPRE do backend.
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Layout } from './components/Layout'
import { rotaInicial, useAuth, type UsuarioAtual } from './context/AuthContext'
import { Login } from './pages/Login/Login'
import { Cadastro } from './pages/Cadastro/Cadastro'
import { EsqueciSenha } from './pages/RecuperarSenha/EsqueciSenha'
import { RedefinirSenha } from './pages/RecuperarSenha/RedefinirSenha'
import { ConfirmarCodigo } from './pages/ConfirmarCodigo/ConfirmarCodigo'
import { Catalogo } from './pages/Catalogo/Catalogo'
import { CatalogoDetalhe } from './pages/Catalogo/CatalogoDetalhe'
import { Inscricoes } from './pages/Inscricoes/Inscricoes'
import { Perfil } from './pages/Perfil/Perfil'
import { Garagem } from './pages/Garagem/Garagem'
import { CarroForm } from './pages/Garagem/CarroForm'
import { CarroDetalhe } from './pages/Garagem/CarroDetalhe'
import { CarroHistorico } from './pages/Garagem/CarroHistorico'
import { ServicosColecionador } from './pages/Servicos/ServicosColecionador'
import { NovoServico } from './pages/Servicos/NovoServico'
import { ServicoDetalhe } from './pages/Servicos/ServicoDetalhe'
import { EventosOrganizador } from './pages/Organizador/EventosOrganizador'
import { EventoForm } from './pages/Organizador/EventoForm'
import { EventoDetalhe } from './pages/Organizador/EventoDetalhe'
import { Dashboard } from './pages/Organizador/Dashboard'
import { PainelDisponiveis } from './pages/Prestador/PainelDisponiveis'
import { FilaTrabalho } from './pages/Prestador/FilaTrabalho'
import { ServicoDetalhePrestador } from './pages/Prestador/ServicoDetalhePrestador'
import { NotFound } from './pages/NotFound'
import type { PrefixoChamado } from './api/servicos'

// ---------------------------------------------------------------------------
// Guardas por perfil
// ---------------------------------------------------------------------------
function ExigirAuth({ children }: { children: ReactNode }) {
  const { autenticado } = useAuth()
  const localizacao = useLocation()
  if (!autenticado) {
    return <Navigate to="/login" state={{ de: localizacao.pathname }} replace />
  }
  return <>{children}</>
}

function ExigirPerfil({ quando, children }: { quando: (u: UsuarioAtual) => boolean; children: ReactNode }) {
  const { usuario } = useAuth()
  if (!usuario || !quando(usuario)) {
    // Perfil errado: manda para a home do próprio perfil (o backend protege de verdade).
    return <Navigate to={rotaInicial(usuario)} replace />
  }
  return <>{children}</>
}

const entusiastaOuColecionador = (u: UsuarioAtual) =>
  u.tipoUsuario === 'ENTUSIASTA' || u.tipoUsuario === 'COLECIONADOR'
const colecionador = (u: UsuarioAtual) => u.tipoUsuario === 'COLECIONADOR'
const organizador = (u: UsuarioAtual) => u.tipoUsuario === 'ORGANIZADOR_EVENTO'
const prestador = (u: UsuarioAtual) => u.tipoUsuario === 'PRESTADOR_SERVICO'

/** Prefixo das rotas de serviço do prestador logado ('mecanicos' | 'guinchos'). */
function PrefixoPrestador({ children }: { children: (prefixo: PrefixoChamado) => ReactNode }) {
  const { usuario } = useAuth()
  const prefixo: PrefixoChamado = usuario?.tipoPrestadorServico === 'MECANICO' ? 'mecanicos' : 'guinchos'
  return <>{children(prefixo)}</>
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* Públicas */}
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/esqueci-senha" element={<EsqueciSenha />} />
        <Route path="/redefinir-senha" element={<RedefinirSenha />} />
        <Route path="/confirmar-codigo" element={<ConfirmarCodigo />} />
        <Route path="/eventos" element={<Catalogo />} />
        <Route path="/eventos/:idEvento" element={<CatalogoDetalhe />} />

        {/* Entusiasta (colecionador mantém os fluxos do entusiasta) */}
        <Route
          path="/inscricoes"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={entusiastaOuColecionador}>
                <Inscricoes />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />

        {/* Perfil: todos os 4 atores */}
        <Route
          path="/perfil"
          element={
            <ExigirAuth>
              <Perfil />
            </ExigirAuth>
          }
        />

        {/* Colecionador */}
        <Route
          path="/garagem"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={entusiastaOuColecionador}>
                <Garagem />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/garagem/novo"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={entusiastaOuColecionador}>
                <CarroForm />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/garagem/:idCarro"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <CarroDetalhe />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/garagem/:idCarro/editar"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <CarroForm />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/garagem/:idCarro/historico"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <CarroHistorico />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/servicos"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <ServicosColecionador />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/servicos/novo-mecanico"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <NovoServico tipo="mecanico" />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/servicos/novo-guincho"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <NovoServico tipo="guincho" />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/servicos/:prefixo/:idServico"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={colecionador}>
                <ServicoDetalhe />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />

        {/* Organizador */}
        <Route
          path="/meus-eventos"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={organizador}>
                <EventosOrganizador />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/meus-eventos/novo"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={organizador}>
                <EventoForm />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/meus-eventos/:idEvento"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={organizador}>
                <EventoDetalhe />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/meus-eventos/:idEvento/editar"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={organizador}>
                <EventoForm />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/dashboard"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={organizador}>
                <Dashboard />
              </ExigirPerfil>
            </ExigirAuth>
          }
        />

        {/* Prestadores (MECANICO/GUINCHO) — mesmo fluxo, prefixo derivado do perfil logado */}
        <Route
          path="/disponiveis"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={prestador}>
                <PrefixoPrestador>{(prefixo) => <PainelDisponiveis prefixo={prefixo} />}</PrefixoPrestador>
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/meus-servicos"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={prestador}>
                <PrefixoPrestador>{(prefixo) => <FilaTrabalho prefixo={prefixo} />}</PrefixoPrestador>
              </ExigirPerfil>
            </ExigirAuth>
          }
        />
        <Route
          path="/prestador/servicos/:idServico"
          element={
            <ExigirAuth>
              <ExigirPerfil quando={prestador}>
                <PrefixoPrestador>{(prefixo) => <ServicoDetalhePrestador prefixo={prefixo} />}</PrefixoPrestador>
              </ExigirPerfil>
            </ExigirAuth>
          }
        />

        <Route path="/" element={<Navigate to="/eventos" replace />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
