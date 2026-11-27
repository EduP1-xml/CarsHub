// Padrão ÚNICO de coordenadas (WGS84), espelho das validações do backend:
// latitude -90..90, longitude -180..180, no máximo 7 casas decimais.
// Use em TODO campo de latitude/longitude para evitar inputs incorretos.

export type TipoCoordenada = 'latitude' | 'longitude'

export const CASAS_DECIMAIS_COORDENADA = 7

const LIMITES: Record<TipoCoordenada, { min: number; max: number }> = {
  latitude: { min: -90, max: 90 },
  longitude: { min: -180, max: 180 },
}

const ROTULO: Record<TipoCoordenada, string> = { latitude: 'A latitude', longitude: 'A longitude' }

// Número decimal simples: sinal opcional, parte inteira e até 7 casas (sem notação científica).
const FORMATO_COORDENADA = /^-?\d{1,3}(\.\d{1,7})?$/

/** Props do <input> de coordenada: o navegador já barra fora do intervalo / casas demais. */
export function propsCampoCoordenada(tipo: TipoCoordenada) {
  return {
    type: 'number',
    inputMode: 'decimal' as const,
    step: '0.0000001',
    min: String(LIMITES[tipo].min),
    max: String(LIMITES[tipo].max),
  }
}

/** Arredonda para 7 casas (mesma precisão que o backend grava). */
export function normalizarCoordenada(valor: number): number {
  return Number(valor.toFixed(CASAS_DECIMAIS_COORDENADA))
}

/** Mensagem de erro do campo, ou null se válido. Campo vazio é válido (= não informado). */
export function validarCoordenada(valor: string, tipo: TipoCoordenada): string | null {
  const texto = valor.trim()
  if (texto === '') return null

  const { min, max } = LIMITES[tipo]
  const intervalo = `${ROTULO[tipo]} deve ser um número entre ${min} e ${max}`

  if (!FORMATO_COORDENADA.test(texto)) {
    return `${intervalo}, com no máximo ${CASAS_DECIMAIS_COORDENADA} casas decimais (ex.: -23.5505200).`
  }
  const numero = Number(texto)
  if (!Number.isFinite(numero) || numero < min || numero > max) {
    return `${intervalo}.`
  }
  return null
}

/** Valida o par e devolve a primeira mensagem de erro encontrada (ou null). */
export function validarParCoordenadas(latitude: string, longitude: string): string | null {
  return validarCoordenada(latitude, 'latitude') ?? validarCoordenada(longitude, 'longitude')
}

/** Converte o texto do campo para o valor enviado à API (null se vazio), já normalizado. */
export function lerCoordenada(valor: string): number | null {
  const texto = valor.trim()
  return texto === '' ? null : normalizarCoordenada(Number(texto))
}

/** Formata a coordenada salva (placeholder etc.). Valor que não é número → null (nunca lança erro). */
export function formatarCoordenada(valor: number | null | undefined): string | null {
  return typeof valor === 'number' && Number.isFinite(valor) ? valor.toFixed(CASAS_DECIMAIS_COORDENADA) : null
}
