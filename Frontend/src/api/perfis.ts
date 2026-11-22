// Endpoints de perfil dos 4 atores (/api/carshub/{entusiastas|organizadores|mecanicos|guinchos}).
import { api } from './client'
import type {
  LatitudeLongitudeDTO,
  PerfilEntusiastaPatchDTO,
  PerfilEntusiastaPutDTO,
  PerfilGuinchoPatchDTO,
  PerfilGuinchoPutDTO,
  PerfilMecanicoPatchDTO,
  PerfilMecanicoPutDTO,
  PerfilOrganizadorEventoPatchDTO,
  PerfilOrganizadorEventoPutDTO,
  PerfilUsuarioDTO,
} from '../types/api'
import type { AlteracaoSenhaDTO } from '../types/api'

export type Ator = 'entusiastas' | 'organizadores' | 'mecanicos' | 'guinchos'

export type PerfilPut =
  | PerfilEntusiastaPutDTO
  | PerfilOrganizadorEventoPutDTO
  | PerfilMecanicoPutDTO
  | PerfilGuinchoPutDTO

export type PerfilPatch =
  | PerfilEntusiastaPatchDTO
  | PerfilOrganizadorEventoPatchDTO
  | PerfilMecanicoPatchDTO
  | PerfilGuinchoPatchDTO

/** GET /{ator}/perfil — PerfilUsuarioDTO (nome, telefone, login, urlFotoPerfil + notas do entusiasta). */
export function buscarPerfil(ator: Ator) {
  return api.get<PerfilUsuarioDTO>(`/${ator}/perfil`)
}

/** PUT /{ator}/perfil — substituição completa (200). */
export function substituirPerfil(ator: Ator, dados: PerfilPut) {
  return api.put<string>(`/${ator}/perfil`, dados)
}

/** PATCH /{ator}/perfil — atualização parcial (campos vazios/nulos = "não alterar"). */
export function atualizarPerfilParcial(ator: Ator, dados: PerfilPatch) {
  return api.patch<string>(`/${ator}/perfil`, dados)
}

/** PUT /{ator}/localizacao — entusiasta aceita um dos dois; prestadores exigem lat E long. 204 sem corpo. */
export function atualizarLocalizacao(ator: Ator, dados: LatitudeLongitudeDTO) {
  return api.put(`/${ator}/localizacao`, dados)
}

/** POST /{ator}/foto-perfil — multipart "arquivo" (1 imagem). 201 → texto com a URL. */
export function enviarFotoPerfil(ator: Ator, arquivo: File) {
  const form = new FormData()
  form.append('arquivo', arquivo)
  return api.post<string>(`/${ator}/foto-perfil`, form)
}

/** POST /{ator}/senha/solicitar-codigo — SEM body, SEM Content-Type de corpo. Cooldown 60 s. */
export function solicitarCodigoSenha(ator: Ator) {
  return api.post<string>(`/${ator}/senha/solicitar-codigo`)
}

/** PATCH /{ator}/senha — { codigo, novaSenha }. Política de senha igual à do cadastro. */
export function alterarSenha(ator: Ator, dados: AlteracaoSenhaDTO) {
  return api.patch<string>(`/${ator}/senha`, dados)
}
