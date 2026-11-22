// Endpoints de autenticação e cadastro (públicos).
// TODOS os métodos mutantes passam pelo interceptor → ganham X-XSRF-TOKEN automaticamente.
import { api } from './client'
import type {
  CadastroDTO,
  CadastroIndividualDTO,
  LoginDTO,
  LoginResponseDTO,
  RedefinicaoSenhaDTO,
  ReenvioCodigoDTO,
  SolicitacaoCodigoSenhaDTO,
  VerificacaoRegistroRequestDTO,
} from '../types/api'

// ---------- Cadastro em 3 etapas ----------
/** Etapa 1: cria o usuário base (e-mail + senha + dados comuns). Resposta 201 = texto. */
export function cadastrarUsuario(dados: CadastroDTO): Promise<{ data: string }> {
  return api.post('/usuarios/cadastrar', dados)
}

/** Etapa 2: completa os dados do perfil (CPF/CNPJ/placa/serviços). Resposta 200 = texto. */
export function cadastrarIndividual(dados: CadastroIndividualDTO): Promise<{ data: string }> {
  return api.post('/usuarios/cadastrar/individual', dados)
}

/** Etapa 3: confirma o código de 6 dígitos. Resposta 200 = texto. */
export function verificarRegistro(dados: VerificacaoRegistroRequestDTO): Promise<{ data: string }> {
  return api.post('/usuarios/cadastrar/verificarRegistro', dados)
}

/** F2 — reenvio do código (cooldown 60 s no backend → 422 "Aguarde X segundo(s)..."). */
export function reenviarCodigo(dados: ReenvioCodigoDTO): Promise<{ data: string }> {
  return api.post('/usuarios/cadastrar/reenviarCodigo', dados)
}

// ---------- Autenticação ----------
/** F3 — login → LoginResponseDTO (JSON com access + refresh). */
export function login(dados: LoginDTO): Promise<{ data: LoginResponseDTO }> {
  return api.post('/autenticacao/login', dados)
}

/** F4 — esqueci minha senha. Responde 200 SEMPRE com a mensagem genérica. */
export function esqueciSenha(dados: SolicitacaoCodigoSenhaDTO): Promise<{ data: string }> {
  return api.post('/autenticacao/esqueci-senha', dados)
}

/** F5 — redefine a senha com o código. E-mail inexistente E código errado → MESMA mensagem 422. */
export function redefinirSenha(dados: RedefinicaoSenhaDTO): Promise<{ data: string }> {
  return api.patch('/autenticacao/senha/redefinir', dados)
}
