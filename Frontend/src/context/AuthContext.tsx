// Estado global de autenticação ("carshub:auth") e perfil do usuário.
// Reage aos eventos do interceptor: refresh bem-sucedido (EVENTO_SESSAO) e
// 401 irrecuperável (EVENTO_LOGOUT → limpa e manda para o login).
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { login as apiLogin } from '../api/auth'
import { EVENTO_LOGOUT, EVENTO_SESSAO } from '../api/client'
import type { LoginDTO, LoginResponseDTO, TipoPrestadorServico, TipoUsuario } from '../types/api'
import {
  lerSessao,
  limparSessao,
  salvarSessao,
  montarSessao,
  atualizarSessao,
  type SessaoAuth,
} from '../utils/sessao'

export interface UsuarioAtual {
  idUsuario: string
  nomeUsuario: string
  tipoUsuario: TipoUsuario
  tipoPrestadorServico: TipoPrestadorServico | null
  urlFotoPerfil: string | null
}

interface AuthContextValue {
  /** Sessão persistida (null = deslogado). */
  sessao: SessaoAuth | null
  usuario: UsuarioAtual | null
  /** true quando há sessão (mesmo com access expirado — o refresh tenta recuperar). */
  autenticado: boolean
  login: (dados: LoginDTO) => Promise<LoginResponseDTO>
  logout: () => void
  /** Espelha mudanças locais da sessão (virada COLECIONADOR↔ENTUSIASTA, foto de perfil). */
  atualizarSessaoLocal: (mudancas: Partial<SessaoAuth>) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<SessaoAuth | null>(() => lerSessao())

  const login = useCallback(async (dados: LoginDTO): Promise<LoginResponseDTO> => {
    const resposta = await apiLogin(dados)
    const novaSessao = montarSessao(resposta.data)
    salvarSessao(novaSessao)
    setSessao(novaSessao)
    return resposta.data
  }, [])

  const logout = useCallback(() => {
    limparSessao()
    setSessao(null)
  }, [])

  const atualizarSessaoLocal = useCallback((mudancas: Partial<SessaoAuth>) => {
    const nova = atualizarSessao(mudancas)
    if (nova) setSessao(nova)
  }, [])

  useEffect(() => {
    const aoRenovar = () => setSessao(lerSessao())
    const aoExpulsar = () => {
      limparSessao()
      setSessao(null)
    }
    const aoMudarStorage = (evento: StorageEvent) => {
      if (evento.key === 'carshub:auth') setSessao(lerSessao())
    }
    window.addEventListener(EVENTO_SESSAO, aoRenovar)
    window.addEventListener(EVENTO_LOGOUT, aoExpulsar)
    window.addEventListener('storage', aoMudarStorage)
    return () => {
      window.removeEventListener(EVENTO_SESSAO, aoRenovar)
      window.removeEventListener(EVENTO_LOGOUT, aoExpulsar)
      window.removeEventListener('storage', aoMudarStorage)
    }
  }, [])

  const usuario = useMemo<UsuarioAtual | null>(() => {
    if (!sessao) return null
    return {
      idUsuario: sessao.idUsuario,
      nomeUsuario: sessao.nomeUsuario,
      tipoUsuario: sessao.tipoUsuario,
      tipoPrestadorServico: sessao.tipoPrestadorServico,
      urlFotoPerfil: sessao.urlFotoPerfil ?? null,
    }
  }, [sessao])

  const valor = useMemo(
    () => ({
      sessao,
      usuario,
      autenticado: !!sessao,
      login,
      logout,
      atualizarSessaoLocal,
    }),
    [sessao, usuario, login, logout, atualizarSessaoLocal],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth deve ser usado dentro de <AuthProvider>')
  return contexto
}

// ---------------------------------------------------------------------------
// Helpers de perfil (roteamento/visibilidade de telas — a regra final é do backend)
// ---------------------------------------------------------------------------
export function ehColecionador(u: UsuarioAtual | null): boolean {
  return u?.tipoUsuario === 'COLECIONADOR'
}

export function ehOrganizador(u: UsuarioAtual | null): boolean {
  return u?.tipoUsuario === 'ORGANIZADOR_EVENTO'
}

export function ehMecanico(u: UsuarioAtual | null): boolean {
  return u?.tipoUsuario === 'PRESTADOR_SERVICO' && u?.tipoPrestadorServico === 'MECANICO'
}

export function ehGuincho(u: UsuarioAtual | null): boolean {
  return u?.tipoUsuario === 'PRESTADOR_SERVICO' && u?.tipoPrestadorServico === 'GUINCHO'
}

/** Rota inicial de cada perfil após o login. */
export function rotaInicial(u: UsuarioAtual | null): string {
  if (!u) return '/login'
  return rotaInicialParaTipo(u.tipoUsuario, u.tipoPrestadorServico)
}

/** Rota inicial calculada direto da resposta do login (antes do estado re-renderizar). */
export function rotaInicialParaTipo(
  tipoUsuario: TipoUsuario,
  tipoPrestadorServico: TipoPrestadorServico | null,
): string {
  if (tipoUsuario === 'ORGANIZADOR_EVENTO') return '/dashboard'
  if (tipoUsuario === 'PRESTADOR_SERVICO' && (tipoPrestadorServico === 'MECANICO' || tipoPrestadorServico === 'GUINCHO')) {
    return '/disponiveis'
  }
  return '/eventos'
}
