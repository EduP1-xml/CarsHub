// Endpoints da garagem do colecionador (/api/carshub/colecionador/carros).
// Multipart padrão: parte "dados" (Blob JSON) + "arquivos"/"foto1"/"foto2" (seção 4 do guia).
import { api, multipartComDados } from './client'
import type {
  CarroPatchDTO,
  CarroRegistroDTO,
  CarroRespostaDTO,
  HistoricoManutencaoResponseDTO,
  PaginaRespostaDTO,
} from '../types/api'

/** POST /colecionador/carros — multipart "dados" + "arquivos" (opcional, máx 2). 201 → CarroRespostaDTO. */
export function registrarCarro(dados: CarroRegistroDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.post<CarroRespostaDTO>('/colecionador/carros', form)
}

/** GET /colecionador/carros?pagina&tamanho — garagem paginada (projeção resumida). */
export function listarCarros(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<CarroRespostaDTO>>('/colecionador/carros', {
    params: { pagina, tamanho },
  })
}

/** GET /colecionador/carros/{idCarro} — detalhe COMPLETO (todos os campos + fotosCarro). */
export function buscarCarro(idCarro: string) {
  return api.get<CarroRespostaDTO>(`/colecionador/carros/${idCarro}`)
}

/** PUT /colecionador/carros/{id} — multipart "dados" + "arquivos" opcional (substitui todas; máx 2). */
export function substituirCarro(idCarro: string, dados: CarroRegistroDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.put<CarroRespostaDTO>(`/colecionador/carros/${idCarro}`, form)
}

/**
 * PATCH /colecionador/carros/{id} — multipart "dados" + "foto1"/"foto2" opcionais.
 * foto1 = índice 0, foto2 = índice 1; troca APENAS o slot correspondente.
 */
export function atualizarCarroParcial(
  idCarro: string,
  dados: CarroPatchDTO,
  foto1: File | null = null,
  foto2: File | null = null,
) {
  const form = multipartComDados(dados)
  if (foto1) form.append('foto1', foto1)
  if (foto2) form.append('foto2', foto2)
  return api.patch<CarroRespostaDTO>(`/colecionador/carros/${idCarro}`, form)
}

/** DELETE /colecionador/carros/{id} — 204 (último carro revoga o título de colecionador). */
export function removerCarro(idCarro: string) {
  return api.delete(`/colecionador/carros/${idCarro}`)
}

/** GET /colecionador/carros/{id}/historico-manutencoes?pagina&tamanho — janela (mec+guincho FINALIZADO). */
export function listarHistoricoManutencoes(idCarro: string, pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<HistoricoManutencaoResponseDTO>>(
    `/colecionador/carros/${idCarro}/historico-manutencoes`,
    { params: { pagina, tamanho } },
  )
}
