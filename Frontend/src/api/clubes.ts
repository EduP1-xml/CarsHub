// Endpoints de clubes (/clubes) — authority ENTUSIASTA (colecionador incluso).
import { api, multipartComDados } from './client'
import type {
  CarroCompativelDTO,
  CarroRespostaDTO,
  CatalogoEventoResponseDTO,
  ClubeRegistroDTO,
  ClubeRespostaDTO,
  MembroClubeRespostaDTO,
  PaginaRespostaDTO,
} from '../types/api'

// ---------- Clube ----------
/** POST /clubes — multipart "dados" + "arquivos" (0 a 5 fotos). 201 → ClubeRespostaDTO. */
export function criarClube(dados: ClubeRegistroDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.post<ClubeRespostaDTO>('/clubes', form)
}

/** GET /clubes?busca&pagina&tamanho — todos os clubes, ordem alfabética. */
export function listarClubes(pagina = 0, tamanho = 5, busca = '') {
  return api.get<PaginaRespostaDTO<ClubeRespostaDTO>>('/clubes', {
    params: { pagina, tamanho, busca: busca.trim() || undefined },
  })
}

/** GET /clubes/meus — clubes de que sou membro (mais recente primeiro). */
export function listarMeusClubes(pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<ClubeRespostaDTO>>('/clubes/meus', { params: { pagina, tamanho } })
}

export function detalharClube(idClube: string) {
  return api.get<ClubeRespostaDTO>(`/clubes/${idClube}`)
}

/** PUT /clubes/{id} — só o admin. Com arquivos, substitui as fotos; sem, mantém "fotosClube". */
export function editarClube(idClube: string, dados: ClubeRegistroDTO, arquivos: File[] = []) {
  const form = multipartComDados(dados)
  arquivos.forEach((arquivo) => form.append('arquivos', arquivo))
  return api.put<ClubeRespostaDTO>(`/clubes/${idClube}`, form)
}

export function excluirClube(idClube: string) {
  return api.delete(`/clubes/${idClube}`)
}

// ---------- Membros ----------
/** POST /clubes/{id}/membros — entrada livre; o colecionador pode já vincular carros. */
export function entrarNoClube(idClube: string, idsCarros: string[] = []) {
  return api.post<ClubeRespostaDTO>(`/clubes/${idClube}/membros`, { idsCarros })
}

export function sairDoClube(idClube: string) {
  return api.delete(`/clubes/${idClube}/membros/eu`)
}

export function removerMembro(idClube: string, idMembro: string) {
  return api.delete(`/clubes/${idClube}/membros/${idMembro}`)
}

export function listarMembros(idClube: string, pagina = 0, tamanho = 10) {
  return api.get<PaginaRespostaDTO<MembroClubeRespostaDTO>>(`/clubes/${idClube}/membros`, {
    params: { pagina, tamanho },
  })
}

/** PATCH /clubes/{id}/administrador/{idNovo} — só o admin atual; o destino precisa ser membro. */
export function transferirAdministracao(idClube: string, idNovoAdministrador: string) {
  return api.patch<ClubeRespostaDTO>(`/clubes/${idClube}/administrador/${idNovoAdministrador}`)
}

// ---------- Carros ----------
/** Carros da MINHA garagem que atendem ao gosto do clube (com a marcação dos já vinculados). */
export function listarMeusCarrosCompativeis(idClube: string) {
  return api.get<CarroCompativelDTO[]>(`/clubes/${idClube}/meus-carros-compativeis`)
}

export function vincularCarro(idClube: string, idCarro: string) {
  return api.post(`/clubes/${idClube}/carros/${idCarro}`)
}

export function desvincularCarro(idClube: string, idCarro: string) {
  return api.delete(`/clubes/${idClube}/carros/${idCarro}`)
}

/** Aba Carros: só os vinculados (sem placa). */
export function listarCarrosDoClube(idClube: string, pagina = 0, tamanho = 6) {
  return api.get<PaginaRespostaDTO<CarroRespostaDTO>>(`/clubes/${idClube}/carros`, {
    params: { pagina, tamanho },
  })
}

// ---------- Eventos ----------
/** Aba Eventos: eventos ATIVOS compatíveis com o gosto do clube. */
export function listarEventosDoClube(idClube: string, pagina = 0, tamanho = 5) {
  return api.get<PaginaRespostaDTO<CatalogoEventoResponseDTO>>(`/clubes/${idClube}/eventos`, {
    params: { pagina, tamanho },
  })
}
