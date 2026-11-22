// Validações locais que ESPELHAM as regras do backend (UX). A regra final é sempre o backend.
export const ESPECIAIS_PERMITIDOS = '@$!%*?&'

export interface ResultadoSenha {
  valida: boolean
  problemas: string[]
}

/** Política de senha forte: mín 8, 1 maiúscula, 1 minúscula, 1 número, 1 especial de @ $ ! % * ? &. */
export function validarSenhaForte(senha: string): ResultadoSenha {
  const problemas: string[] = []
  if (senha.length < 8) problemas.push('no mínimo 8 caracteres')
  if (!/[A-Z]/.test(senha)) problemas.push('1 letra maiúscula')
  if (!/[a-z]/.test(senha)) problemas.push('1 letra minúscula')
  if (!/\d/.test(senha)) problemas.push('1 número')
  if (!new RegExp(`[${ESPECIAIS_PERMITIDOS.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}]`).test(senha)) {
    problemas.push(`1 caractere especial do conjunto @ $ ! % * ? &`)
  }
  return { valida: problemas.length === 0, problemas }
}

export const REGRAS_SENHA_TEXTO =
  'A senha deve ter no mínimo 8 caracteres, com 1 maiúscula, 1 minúscula, 1 número e 1 especial do conjunto @ $ ! % * ? &.'

/** Máscaras leves de digitação (espelho; o backend valida o formato final). */
export function apenasDigitos(valor: string, maximo: number): string {
  return valor.replace(/\D/g, '').slice(0, maximo)
}

export function apenasAlfanumericos(valor: string, maximo: number): string {
  return valor.replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, maximo)
}

export function apenasDigitosCodigo(valor: string, maximo = 6): string {
  return valor.replace(/\D/g, '').slice(0, maximo)
}

/** "2026-11-09T10:00" (datetime-local) → "2026-11-09T10:00:00" (LocalDateTime ISO sem offset). */
export function paraIsoLocalDateTime(valor: string): string {
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(valor)) return `${valor}:00`
  return valor
}

/** "2026-11-09T10:00:00" → "2026-11-09T10:00" (para o input datetime-local). */
export function paraInputDatetimeLocal(iso: string | null | undefined): string {
  if (!iso) return ''
  return iso.slice(0, 16)
}

/**
 * Máscara dd/mm/aaaa de digitação: insere as barras conforme os dígitos chegam.
 * Respeita APAGAR: se o usuário removeu caracteres (não digitou), devolve o
 * valor como veio — assim dá para apagar o dia, o mês ou o ano e corrigir.
 */
export function mascaraData(valor: string, anterior: string): string {
  const digitosNovo = valor.replace(/\D/g, '')
  const digitosAnterior = anterior.replace(/\D/g, '')
  if (digitosNovo.length <= digitosAnterior.length) return valor

  const digitos = digitosNovo.slice(0, 8)
  let resultado = digitos
  if (digitos.length > 4) resultado = `${digitos.slice(0, 2)}/${digitos.slice(2, 4)}/${digitos.slice(4)}`
  else if (digitos.length > 2) resultado = `${digitos.slice(0, 2)}/${digitos.slice(2)}`
  return resultado
}

/** dd/mm/aaaa completo e válido → true (espelho; o backend valida o LocalDate). */
export function dataBrasileiraValida(valor: string): boolean {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor.trim())
  if (!m) return false
  const dia = Number(m[1])
  const mes = Number(m[2])
  const ano = Number(m[3])
  if (mes < 1 || mes > 12 || dia < 1 || dia > 31) return false
  const data = new Date(ano, mes - 1, dia)
  return data.getFullYear() === ano && data.getMonth() === mes - 1 && data.getDate() === dia
}

/** dd/mm/aaaa → "aaaa-mm-dd" (ISO que o backend LocalDate espera); inválida → null. */
export function dataBrasileiraParaIso(valor: string): string | null {
  if (!dataBrasileiraValida(valor)) return null
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor.trim())!
  return `${m[3]}-${m[2]}-${m[1]}`
}
