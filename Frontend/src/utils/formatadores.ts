// Formatação de valores para exibição. Datas do backend são LocalDateTime ISO SEM offset
// (ex.: "2026-11-01T10:00:00") — DEVEM ser tratadas como hora local, sem conversão de fuso.

const pad2 = (n: number) => String(n).padStart(2, '0')

/** "2026-11-01T10:00:00" → "01/11/2026 10:00" (hora local, sem offset) */
export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  if (!m) return iso
  return `${m[3]}/${m[2]}/${m[1]} ${m[4]}:${m[5]}`
}

/** "1990-05-20" → "20/05/1990" */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '—'
  const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!m) return iso
  return `${m[3]}/${m[2]}/${m[1]}`
}

/** Preço com 2 casas decimais (R$). */
export function formatarMoeda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—'
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** Distância em km com 1 casa decimal + unidade. */
export function formatarDistancia(km: number | null | undefined): string {
  if (km === null || km === undefined) return '—'
  return `${km.toFixed(1)} km`
}

/** Nota média (ajustada) com 2 casas. */
export function formatarNota(nota: number | null | undefined): string {
  if (nota === null || nota === undefined) return '—'
  return nota.toFixed(2)
}

/**
 * dataHoraPendente + 2 horas (regra R6/N3 do cancelamento em PENDENTE).
 * Usada APENAS para espelhar a regra na UI; a regra final é do backend (422 com minutos restantes).
 */
export function minutosRestantesCancelamento(dataHoraPendente: string | null | undefined): number {
  if (!dataHoraPendente) return 0
  const m = dataHoraPendente.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  if (!m) return 0
  const alvo = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]))
  const disponivelEm = alvo.getTime() + 2 * 60 * 60 * 1000
  const restante = disponivelEm - Date.now()
  return restante > 0 ? Math.ceil(restante / 60000) : 0
}

/** CEP "01310100" → "01310-100" */
export function formatarCep(cep: string | null | undefined): string {
  if (!cep) return '—'
  if (/^\d{8}$/.test(cep)) return `${cep.slice(0, 5)}-${cep.slice(5)}`
  return cep
}
