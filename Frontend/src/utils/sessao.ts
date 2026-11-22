// "Sessão de tokens" (termo do guia): conjunto persistido no cliente sob a chave
// ÚNICA `carshub:auth` em localStorage.
//   { accessToken, refreshToken, expiraEm, idUsuario, nomeUsuario, tipoUsuario, tipoPrestadorServico }
// expiraEm = Date.now() + expiraEmSegundos * 1000 (calculado no login e a cada refresh).
import type { LoginResponseDTO, TipoPrestadorServico, TipoUsuario } from '../types/api'

export const CHAVE_SESSAO = 'carshub:auth'

export interface SessaoAuth {
  accessToken: string
  refreshToken: string
  /** Timestamp (ms) em que o accessToken expira. */
  expiraEm: number
  idUsuario: string
  nomeUsuario: string
  tipoUsuario: TipoUsuario
  tipoPrestadorServico: TipoPrestadorServico | null
  /** Conforto de UI (avatar); não faz parte do contrato do backend. */
  urlFotoPerfil?: string | null
}

export function montarSessao(resposta: LoginResponseDTO): SessaoAuth {
  return {
    accessToken: resposta.accessToken,
    refreshToken: resposta.refreshToken,
    expiraEm: Date.now() + resposta.expiraEmSegundos * 1000,
    idUsuario: resposta.idUsuario,
    nomeUsuario: resposta.nomeUsuario,
    tipoUsuario: resposta.tipoUsuario,
    tipoPrestadorServico: resposta.tipoPrestadorServico,
  }
}

export function lerSessao(): SessaoAuth | null {
  try {
    const bruto = localStorage.getItem(CHAVE_SESSAO)
    if (!bruto) return null
    const sessao = JSON.parse(bruto) as SessaoAuth
    if (!sessao?.accessToken || !sessao?.refreshToken) return null
    return sessao
  } catch {
    return null
  }
}

export function salvarSessao(sessao: SessaoAuth): void {
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao))
}

export function limparSessao(): void {
  localStorage.removeItem(CHAVE_SESSAO)
}

/** true quando o accessToken ainda é considerado válido (expiraEm > agora). */
export function accessTokenValido(sessao: SessaoAuth | null): boolean {
  return !!sessao && sessao.expiraEm > Date.now()
}

export function atualizarSessao(mudancas: Partial<SessaoAuth>): SessaoAuth | null {
  const atual = lerSessao()
  if (!atual) return null
  const nova = { ...atual, ...mudancas }
  salvarSessao(nova)
  return nova
}

// ---------------------------------------------------------------------------
// Virada do título COLECIONADOR (regra da seção 5 do guia): o título nasce ao
// cadastrar o 1º carro e morre ao excluir o último. O idUsuario/token NÃO mudam.
// A UI espelha a virada localmente (a regra final é sempre do backend).
// ---------------------------------------------------------------------------
export function marcarComoColecionador(): SessaoAuth | null {
  return atualizarSessao({ tipoUsuario: 'COLECIONADOR' })
}

export function marcarComoEntusiasta(): SessaoAuth | null {
  return atualizarSessao({ tipoUsuario: 'ENTUSIASTA' })
}
