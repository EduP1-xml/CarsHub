// ============================================================================
// CAMADA HTTP ÚNICA (seção 2 do guia) — TODA chamada passa por aqui.
// PROIBIDO axios/fetch avulso dentro de pages/components.
//
//  • Base URL única e constante
//  • withCredentials: true (cookie XSRF-TOKEN cross-origin 5173 → 8080)
//  • Authorization: Bearer <accessToken> SÓ quando expiraEm > Date.now()
//      (o filtro JWT valida o header SEMPRE que ele existe — inclusive em rotas
//       públicas: token expirado em rota pública responde 401. Por isso, em
//       rota pública com token vencido NÃO anexamos o header.)
//  • CSRF double-submit: "garantia do cookie CSRF" antes de TODO método
//      mutante (POST/PUT/PATCH/DELETE) + header X-XSRF-TOKEN.
//  • 401 em rota protegida → refresh UMA única vez ("repetição única
//      pós-refresh") e repetição da requisição original com o novo token.
//  • 429 → expõe retryAfter no erro para a UI respeitar o Retry-After.
// ============================================================================
import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig, type RawAxiosHeaders } from 'axios'
import { getCookie } from '../utils/cookies'
import type { SessaoAuth } from '../utils/sessao'
import { lerSessao, limparSessao, salvarSessao, montarSessao } from '../utils/sessao'
import type { LoginResponseDTO, RenovacaoTokenDTO } from '../types/api'

export const API_BASE = 'http://localhost:8080/api/carshub'

/** Evento disparado quando o refresh falha (401 irrecuperável): AuthContext limpa e redireciona ao login. */
export const EVENTO_LOGOUT = 'carshub:logout'
/** Evento disparado quando a sessão é renovada pelo interceptor: AuthContext re-sincroniza. */
export const EVENTO_SESSAO = 'carshub:sessao'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** Marca a requisição como "já tentou refresh" — nunca repetir duas vezes a mesma. */
    _retry?: boolean
    /** Não anexa Authorization (usado pelo próprio refresh: rota pública com token possivelmente inválido). */
    _skipAuth?: boolean
    /** Requisição já repetida por CSRF (bootstrap) — nunca repetir duas vezes a mesma. */
    _csrfRetry?: boolean
    /** true quando o interceptor de requisição anexou X-XSRF-TOKEN. */
    _csrfAnexado?: boolean
  }
}

export const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
})

// ---------------------------------------------------------------------------
// Garantia do cookie CSRF (seção 2.2 do guia) + comportamento REAL do backend.
//
// OBSERVAÇÃO EMPIRICAMENTE VALIDADA (09/11/2026, backend Sprint 6):
//  • o backend NÃO devolve o cookie XSRF-TOKEN em GET (CSRF deferido do Spring);
//  • o cookie NASCE na resposta do PRIMEIRO mutante negado (403 + Set-Cookie);
//  • toda requisição AUTENTICADA bem-sucedida APAGA o cookie na resposta
//    (CsrfAuthenticationStrategy + STATELESS); o token em si continua válido
//    (double-submit stateless: comparação header == cookie).
// Portanto o cliente: (1) faz o GET público da "garantia" (mantido, conforme o
// guia); (2) memoriza o valor do token em sessionStorage; (3) re-grava o cookie
// quando o backend o apaga; (4) em 403 de mutação com cookie recém-chegado,
// repete UMA vez com o header (bootstrap — o próprio backend entrega o token).
// ---------------------------------------------------------------------------
const CHAVE_CSRF = 'carshub:csrf'
let promessaCsrf: Promise<void> | null = null

function memorizarCsrf(): void {
  const valor = getCookie('XSRF-TOKEN')
  if (valor) sessionStorage.setItem(CHAVE_CSRF, valor)
}

async function garantirCookieCsrf(): Promise<void> {
  if (getCookie('XSRF-TOKEN')) return

  const memorizado = sessionStorage.getItem(CHAVE_CSRF)
  if (memorizado) {
    // O backend apagou o cookie (resposta de requisição autenticada); o mesmo
    // token continua válido — re-gravamos para o próximo mutante não levar 403.
    document.cookie = `XSRF-TOKEN=${memorizado}; path=/; SameSite=Lax`
    return
  }

  if (!promessaCsrf) {
    promessaCsrf = axios
      .get(`${API_BASE}/eventos`, { params: { pagina: 0, tamanho: 1 }, withCredentials: true })
      .then(() => undefined)
      .catch(() => undefined) // mesmo se falhar, seguimos: o backend devolverá o 403 de CSRF se o header faltar
      .finally(() => {
        promessaCsrf = null
      })
  }
  return promessaCsrf
}

const METODOS_MUTANTES = new Set(['post', 'put', 'patch', 'delete'])

// ---------------------------------------------------------------------------
// Interceptor de REQUISIÇÃO
// ---------------------------------------------------------------------------
api.interceptors.request.use(async (config) => {
  const sessao = lerSessao()

  // REGRA DO ACCESS TOKEN: anexar Authorization APENAS quando o token ainda é
  // considerado válido (expiraEm > Date.now()). _skipAuth desliga isso (refresh).
  if (!config._skipAuth && sessao && sessao.expiraEm > Date.now()) {
    config.headers.set('Authorization', `Bearer ${sessao.accessToken}`)
  }

  // CSRF em TODO método mutante (inclusive rotas públicas mutantes:
  // login, refresh, cadastro, esqueci-senha, redefinir senha).
  const metodo = (config.method ?? 'get').toLowerCase()
  if (METODOS_MUTANTES.has(metodo)) {
    await garantirCookieCsrf()
    const csrf = getCookie('XSRF-TOKEN')
    if (csrf) {
      config.headers.set('X-XSRF-TOKEN', csrf)
      config._csrfAnexado = true
    } else {
      config._csrfAnexado = false
    }
  }

  return config
})

// ---------------------------------------------------------------------------
// Refresh single-flight: N 401 concorrentes → 1 única chamada de refresh.
// O refresh é método MUTANTE → também envia X-XSRF-TOKEN (F3b).
// _skipAuth garante que um accessToken inválido (mas não expirado) não seja
// anexado à rota pública de refresh (o filtro JWT validaria e responderia 401).
// ---------------------------------------------------------------------------
let refreshEmAndamento: Promise<SessaoAuth> | null = null

async function renovarParDeTokens(refreshToken: string): Promise<SessaoAuth> {
  if (!refreshEmAndamento) {
    refreshEmAndamento = (async () => {
      const corpo: RenovacaoTokenDTO = { refreshToken }
      const resposta = await api.post<LoginResponseDTO>('/autenticacao/refresh', corpo, {
        _skipAuth: true,
        _retry: true,
      } as AxiosRequestConfig)
      const sessao = montarSessao(resposta.data)
      salvarSessao(sessao)
      return sessao
    })().finally(() => {
      refreshEmAndamento = null
    })
  }
  return refreshEmAndamento
}

// ---------------------------------------------------------------------------
// Interceptor de RESPOSTA — 401 com "repetição única pós-refresh" + 429
// ---------------------------------------------------------------------------
api.interceptors.response.use(
  (resposta) => {
    // Memoriza o token CSRF recém-chegado (o backend pode apagá-lo na próxima
    // resposta autenticada — a memória permite re-gravar sem novo 403).
    memorizarCsrf()
    return resposta
  },
  async (error: AxiosError) => {
    // Sem resposta = rede/backend fora (não culpar o front, mas avisar).
    if (!error.response) {
      ;(error as AxiosError & { erroDeRede?: boolean }).erroDeRede = true
      return Promise.reject(error)
    }

    const status = error.response.status
    memorizarCsrf()

    // 429: anexar o retryAfter (segundos do header Retry-After) para a UI respeitar.
    if (status === 429) {
      const bruto = error.response.headers?.['retry-after'] as string | undefined
      const segundos = bruto ? parseInt(bruto, 10) : NaN
      ;(error as AxiosError & { retryAfter?: number | null }).retryAfter = Number.isFinite(segundos)
        ? segundos
        : null
      return Promise.reject(error)
    }

    // Bootstrap CSRF: o backend emite o cookie XSRF-TOKEN na resposta do PRIMEIRO
    // mutante negado (403). Se a mutação saiu sem header (cookie ainda não
    // existia) e agora o cookie existe, repetimos UMA vez com o valor recém-emitido.
    if (status === 403) {
      const config403 = error.config as AxiosRequestConfig | undefined
      const metodo403 = (config403?.method ?? 'get').toLowerCase()
      if (METODOS_MUTANTES.has(metodo403) && !config403?._csrfRetry && !config403?._csrfAnexado) {
        const csrf = getCookie('XSRF-TOKEN')
        if (csrf) {
          sessionStorage.setItem(CHAVE_CSRF, csrf)
          const configNova: AxiosRequestConfig = { ...config403, _csrfRetry: true, _csrfAnexado: true }
          configNova.headers = AxiosHeaders.from(configNova.headers as RawAxiosHeaders | undefined)
          configNova.headers.set('X-XSRF-TOKEN', csrf)
          return api(configNova)
        }
      }
      return Promise.reject(error)
    }

    if (status !== 401) return Promise.reject(error)

    const config = error.config as AxiosRequestConfig | undefined
    const url = config?.url ?? ''

    // Rotas de autenticação (login/refresh/esqueci/redefinir) NÃO entram no
    // fluxo de refresh — o 401 de lá é a resposta legítima do backend
    // ("Credenciais inválidas.", "Conta não verificada...", token refresh ruim).
    const ehRotaDeAutenticacao = url.startsWith('/autenticacao/')
    if (ehRotaDeAutenticacao) return Promise.reject(error)

    // Repetição única: nunca repetir duas vezes a mesma requisição.
    if (!config || config._retry) return Promise.reject(error)

    const sessao = lerSessao()
    if (!sessao?.refreshToken) {
      // Rota protegida sem token → 401 legítimo ("Acesso negado: autenticação necessária.").
      return Promise.reject(error)
    }

    try {
      const novaSessao = await renovarParDeTokens(sessao.refreshToken)
      // Repete a requisição original com o novo accessToken.
      const configNova: AxiosRequestConfig = { ...config, _retry: true, _skipAuth: false }
      configNova.headers = AxiosHeaders.from(config.headers as RawAxiosHeaders | undefined)
      configNova.headers.set('Authorization', `Bearer ${novaSessao.accessToken}`)
      window.dispatchEvent(new Event(EVENTO_SESSAO))
      return api(configNova)
    } catch (erroRefresh) {
      // Refresh também falhou (401 irrecuperável): limpar carshub:auth e ir para o login.
      limparSessao()
      window.dispatchEvent(new Event(EVENTO_LOGOUT))
      return Promise.reject(erroRefresh)
    }
  },
)

// ---------------------------------------------------------------------------
// Helper multipart: a parte JSON chama-se "dados" e DEVE ser um Blob
// (application/json). PROIBIDO definir Content-Type manualmente em FormData
// (o navegador gera o boundary).
// ---------------------------------------------------------------------------
export function multipartComDados(dados: object): FormData {
  const formData = new FormData()
  formData.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
  return formData
}

/** Pré-validação de imagem (espelho de UX; a regra final é do backend — 415/413/400). */
export function validarImagemLocal(arquivo: File): string | null {
  const extensao = arquivo.name.toLowerCase().split('.').pop() ?? ''
  if (!['jpg', 'jpeg', 'png'].includes(extensao)) {
    return 'Formato de arquivo inválido. Por favor, envie apenas imagens (JPG, JPEG ou PNG).'
  }
  if (arquivo.size > 100 * 1024 * 1024) {
    return 'O arquivo excede o limite de 100MB.'
  }
  if (arquivo.name.includes('..')) {
    return 'Nome de arquivo inválido.'
  }
  return null
}
