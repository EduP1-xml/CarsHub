// Endpoints de eventos: catálogo público (/eventos) e CRUD do organizador (/organizadores/eventos).
import { api, multipartComDados } from './client'
import type {
  CatalogoEventoResponseDTO,
  EventoPatchDTO,
  EventoPutDTO,
  EventoRegistroDTO,
  EventoRespostaDTO,
  PaginaRespostaDTO,
  StatusEvento,
} from '../types/api'

// ---------- Catálogo público (Authorization OPCIONAL — o interceptor só anexa token VÁLIDO) ----------
/** GET /eventos?pagina&tamanho — feed ATIVO, dataHoraInicio asc, com fotos e flag inscrito. */
export function listarCatalogo(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<CatalogoEventoResponseDTO>>('/eventos', {
    params: { pagina, tamanho },
  })
}

/** GET /eventos/{id} — detalhe público (404 "Evento não encontrado." para evento não-ATIVO). */
export function detalharEventoPublico(idEvento: string) {
  return api.get<CatalogoEventoResponseDTO>(`/eventos/${idEvento}`)
}

// ---------- CRUD do organizador ----------
/** POST /organizadores/eventos — multipart "dados" + "arquivos" (máx 3). 201 → EventoRespostaDTO. */
export function criarEvento(dados: EventoRegistroDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.post<EventoRespostaDTO>('/organizadores/eventos', form)
}

/** GET /organizadores/eventos?pagina&tamanho — meus eventos, dataHoraInicio desc. */
export function listarMeusEventos(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<EventoRespostaDTO>>('/organizadores/eventos', {
    params: { pagina, tamanho },
  })
}

/** GET /organizadores/eventos/{id} — detalhe (só do dono). */
export function buscarMeuEvento(idEvento: string) {
  return api.get<EventoRespostaDTO>(`/organizadores/eventos/${idEvento}`)
}

/** PUT /organizadores/eventos/{id} — multipart "dados" (EventoPutDTO) + "arquivos" opcional (máx 3). */
export function substituirEvento(idEvento: string, dados: EventoPutDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.put<EventoRespostaDTO>(`/organizadores/eventos/${idEvento}`, form)
}

/** PATCH /organizadores/eventos/{id} — multipart "dados" (EventoPatchDTO, campos opcionais) + "arquivos" opcional. */
export function atualizarEventoParcial(idEvento: string, dados: EventoPatchDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.patch<EventoRespostaDTO>(`/organizadores/eventos/${idEvento}`, form)
}

/** PATCH /organizadores/eventos/{id}/status?novoStatus=... — 5 valores, sem validação de transição. */
export function alterarStatusEvento(idEvento: string, novoStatus: StatusEvento) {
  return api.patch<EventoRespostaDTO>(`/organizadores/eventos/${idEvento}/status`, null, {
    params: { novoStatus },
  })
}

/** DELETE /organizadores/eventos/{id} — 204; com inscrições → 409 (orientar CANCELADO). */
export function excluirEvento(idEvento: string) {
  return api.delete(`/organizadores/eventos/${idEvento}`)
}

// ---------- Dashboard ----------
export interface MetricasResponse {
  totalEventosCriados: number
  totalParticipantesMobilizados: number
}

export function buscarMetricasDashboard() {
  return api.get<MetricasResponse>('/organizadores/dashboard/metricas')
}

export function listarHistoricoDashboard(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<{
    idEvento: string
    nome: string
    status: StatusEvento
    limiteVagas: number
    totalParticipantes: number
  }>>('/organizadores/dashboard/historico', { params: { pagina, tamanho } })
}
