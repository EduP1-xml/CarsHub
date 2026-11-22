// Endpoints de serviços — colecionador (chamados) e prestadores (painel/fila).
// "PrefixoChamado" = 'mecanicos' | 'guinchos' (mesmas rotas nos dois tipos).
import { api } from './client'
import type {
  AvaliacaoServicoDTO,
  PaginaRespostaDTO,
  PrestadorCompativelDTO,
  PropostaServicoDTO,
  ServicoDisponivelDTO,
  ServicoGuinchoRegistroDTO,
  ServicoMecanicoRegistroDTO,
  ServicoRespostaDTO,
} from '../types/api'

export type PrefixoChamado = 'mecanicos' | 'guinchos'

// ---------------------------------------------------------------------------
// Colecionador — abrir chamado
// ---------------------------------------------------------------------------
/** F14 — POST /colecionador/servicos/mecanicos — 201 (SOLICITADO). */
export function solicitarServicoMecanico(dados: ServicoMecanicoRegistroDTO) {
  return api.post<ServicoRespostaDTO>('/colecionador/servicos/mecanicos', dados)
}

/** F15 — POST /colecionador/servicos/guinchos — 201 (SOLICITADO). */
export function solicitarServicoGuincho(dados: ServicoGuinchoRegistroDTO) {
  return api.post<ServicoRespostaDTO>('/colecionador/servicos/guinchos', dados)
}

// ---------------------------------------------------------------------------
// Colecionador — meus chamados
// ---------------------------------------------------------------------------
/** Lista específica (mecânico OU guincho): somenteAbertos default true (sem FINALIZADO). */
export function listarMeusChamados(prefixo: PrefixoChamado, somenteAbertos = true, pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<ServicoRespostaDTO>>(`/colecionador/servicos/${prefixo}`, {
    params: { somenteAbertos, pagina, tamanho },
  })
}

/** F16/F25 — painel consolidado (mecânico + guincho, status ≠ FINALIZADO, janela, dataHoraSolicitacao desc). */
export function listarPainelConsolidado(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<ServicoRespostaDTO>>('/colecionador/servicos', {
    params: { pagina, tamanho },
  })
}

/** Detalhe do chamado (colecionador). tipoServico vem no DTO — o chamador informa o prefixo da rota. */
export function buscarMeuChamado(prefixo: PrefixoChamado, idServico: string) {
  return api.get<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}`)
}

// Ações do chamado (colecionador) — F18–F24
export function aceitarProposta(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/aceitar-proposta`)
}
export function rejeitarProposta(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/rejeitar-proposta`)
}
export function enviarFotoInicial(prefixo: PrefixoChamado, idServico: string, arquivo: File) {
  const form = new FormData()
  form.append('arquivo', arquivo)
  return api.post<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/foto-inicial`, form)
}
export function confirmarChegadaColecionador(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/confirmar-chegada`)
}
export function enviarFotoFinalColecionador(prefixo: PrefixoChamado, idServico: string, arquivo: File) {
  const form = new FormData()
  form.append('arquivo', arquivo)
  return api.post<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/foto-final`, form)
}
export function avaliarPrestador(prefixo: PrefixoChamado, idServico: string, dados: AvaliacaoServicoDTO) {
  return api.post<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/avaliacao`, dados)
}
export function confirmarConclusaoColecionador(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/colecionador/servicos/${prefixo}/${idServico}/confirmar-conclusao`)
}
/** F24 — cancelamento (DELETE físico; PENDENTE só após 2 h). 204 sem corpo. */
export function cancelarChamado(prefixo: PrefixoChamado, idServico: string) {
  return api.delete(`/colecionador/servicos/${prefixo}/${idServico}`)
}

/** F26 — ranking de prestadores compatíveis (ordem vem pronta do backend — não reordenar). */
export function listarPrestadoresCompativeis(idServico: string, pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<PrestadorCompativelDTO>>(
    `/colecionador/servicos/${idServico}/prestadores-compativeis`,
    { params: { pagina, tamanho } },
  )
}

// ---------------------------------------------------------------------------
// Prestador (mecânico/guincho)
// ---------------------------------------------------------------------------
export interface FiltrosDisponiveis {
  raioKm?: number | null
  latitude?: number | null
  longitude?: number | null
  endereco?: string | null
}

/** F36 — painel de chamados disponíveis (ordem por distância asc + nome — vem pronta). */
export function listarDisponiveis(
  prefixo: PrefixoChamado,
  filtros: FiltrosDisponiveis,
  pagina = 0,
  tamanho = 5,
) {
  return api.get<PaginaRespostaDTO<ServicoDisponivelDTO>>(`/${prefixo}/servicos/disponiveis`, {
    params: {
      raioKm: filtros.raioKm ?? undefined,
      latitude: filtros.latitude ?? undefined,
      longitude: filtros.longitude ?? undefined,
      endereco: filtros.endereco || undefined,
      pagina,
      tamanho,
    },
  })
}

/** F37 — fila de trabalho (ordem PENDENTE → CHEGOU → PROPOSTA → FINALIZADO vem pronta). */
export function listarFilaDeTrabalho(prefixo: PrefixoChamado, pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<ServicoRespostaDTO>>(`/${prefixo}/servicos`, {
    params: { pagina, tamanho },
  })
}

/** F38 — detalhe do chamado (só se participa). */
export function buscarChamadoPrestador(prefixo: PrefixoChamado, idServico: string) {
  return api.get<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}`)
}

// Ações do prestador — F39–F43
export function enviarProposta(prefixo: PrefixoChamado, idServico: string, dados: PropostaServicoDTO) {
  return api.post<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}/proposta`, dados)
}
export function confirmarChegadaPrestador(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}/confirmar-chegada`)
}
export function enviarFotoFinalPrestador(prefixo: PrefixoChamado, idServico: string, arquivo: File) {
  const form = new FormData()
  form.append('arquivo', arquivo)
  return api.post<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}/foto-final`, form)
}
export function avaliarColecionador(prefixo: PrefixoChamado, idServico: string, dados: AvaliacaoServicoDTO) {
  return api.post<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}/avaliacao`, dados)
}
export function confirmarConclusaoPrestador(prefixo: PrefixoChamado, idServico: string) {
  return api.patch<ServicoRespostaDTO>(`/${prefixo}/servicos/${idServico}/confirmar-conclusao`)
}
