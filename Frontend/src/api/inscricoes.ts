// Endpoints de inscrições do entusiasta (/api/carshub/entusiastas/inscricoes).
import { api } from './client'
import type { InscricaoRegistroDTO, InscricaoResponseDTO, PaginaRespostaDTO } from '../types/api'

/** F11 — POST /entusiastas/inscricoes/{idEvento} — o id vai na URL; corpo só { idCarro? }. 201 CONFIRMADA. */
export function inscreverEmEvento(idEvento: string, dados: InscricaoRegistroDTO = {}) {
  return api.post<InscricaoResponseDTO>(`/entusiastas/inscricoes/${idEvento}`, dados)
}

/** F12 — GET /entusiastas/inscricoes?pagina&tamanho — PAGINADA (data desc). */
export function listarMinhasInscricoes(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<InscricaoResponseDTO>>('/entusiastas/inscricoes', {
    params: { pagina, tamanho },
  })
}

/** F12 — PATCH /entusiastas/inscricoes/{idInscricao}/cancelar — NÃO apaga; status vira CANCELADA e a vaga volta. */
export function cancelarInscricao(idInscricao: string) {
  return api.patch<string>(`/entusiastas/inscricoes/${idInscricao}/cancelar`)
}
