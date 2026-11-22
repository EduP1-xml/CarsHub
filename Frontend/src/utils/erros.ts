// Tratamento de erros — regra de ouro: NUNCA trocar/esconder/reescrever mensagens
// do backend. O texto vem pronto para o usuário final (pt-BR).

export interface ErroNormalizado {
  status: number | null
  /** Mensagem única para exibir (texto puro do backend, sem traduzir). */
  mensagem: string | null
  /** Mapa {campo: mensagem} quando o backend respondeu 400 com validação por campo. */
  campos: Record<string, string> | null
  /** Segundos do header Retry-After (429). */
  retryAfter: number | null
  /** true quando não houve resposta (backend fora do ar / CORS / rede). */
  erroDeRede: boolean
}

function ehMapaDeCampos(valor: unknown): valor is Record<string, string> {
  return (
    typeof valor === 'object' &&
    valor !== null &&
    !Array.isArray(valor) &&
    Object.values(valor).every((v) => typeof v === 'string')
  )
}

export function extrairErro(error: unknown): ErroNormalizado {
  // Já normalizado (ex.: erros vindos do hook de paginação) — devolve como está.
  if (
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    ('mensagem' in error || 'campos' in error)
  ) {
    const ja = error as ErroNormalizado
    return {
      status: ja.status,
      mensagem: ja.mensagem ?? null,
      campos: ja.campos ?? null,
      retryAfter: ja.retryAfter ?? null,
      erroDeRede: ja.erroDeRede ?? false,
    }
  }

  const qualquer = error as {
    response?: { status?: number; data?: unknown; headers?: Record<string, unknown> }
    retryAfter?: number | null
    message?: string
    erroDeRede?: boolean
  }

  if (qualquer?.erroDeRede || !qualquer?.response) {
    return {
      status: null,
      mensagem: 'Não foi possível conectar ao servidor. Verifique se o backend está no ar e tente novamente.',
      campos: null,
      retryAfter: null,
      erroDeRede: true,
    }
  }

  const status = qualquer.response.status ?? null
  const dados = qualquer.response.data
  let retryAfter = qualquer.retryAfter ?? null

  if (status === 429) {
    const ra = (qualquer.response.headers as Record<string, string> | undefined)?.['retry-after']
    const num = ra ? parseInt(ra, 10) : NaN
    if (Number.isFinite(num)) retryAfter = num
  }

  // 400 de validação: mapa {campo: mensagem} (R13)
  if (status === 400 && ehMapaDeCampos(dados)) {
    return { status, mensagem: null, campos: dados, retryAfter, erroDeRede: false }
  }

  // Demais erros: texto puro do backend (422, 401, 403, 404, 409, 429, 500, 502/503/504...)
  if (typeof dados === 'string' && dados.trim().length > 0) {
    return { status, mensagem: dados, campos: null, retryAfter, erroDeRede: false }
  }

  // JSON inesperado: tenta campos comuns, senão mensagem genérica sem inventar texto do backend
  if (dados && typeof dados === 'object') {
    const obj = dados as Record<string, unknown>
    const msg = obj.message ?? obj.mensagem ?? obj.erro ?? obj.error ?? obj.detail
    if (typeof msg === 'string') {
      return { status, mensagem: msg, campos: null, retryAfter, erroDeRede: false }
    }
  }

  return {
    status,
    mensagem: 'Erro inesperado no servidor. Tente novamente.',
    campos: null,
    retryAfter,
    erroDeRede: false,
  }
}

/** Formata um ErroNormalizado para exibição em alerta (mensagem única). */
export function mensagemDoErro(error: unknown): string {
  const e = extrairErro(error)
  if (e.mensagem) return e.mensagem
  if (e.campos) {
    const primeira = Object.entries(e.campos)[0]
    if (primeira) return primeira[1]
    return 'Verifique os campos destacados no formulário.'
  }
  return 'Erro inesperado. Tente novamente.'
}
