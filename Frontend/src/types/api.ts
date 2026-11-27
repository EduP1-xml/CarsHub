// ============================================================================
// Espelho TYPESCRIPT dos DTOs Java do backend (models/dtos/**).
// Os nomes dos campos são os EXATOS nomes JSON (P1: pt-BR camelCase).
// NUNCA inventar campos: o DTO Java é a verdade.
// ============================================================================

// ---------- Enums (valores exatos do JSON) ----------
export type TipoUsuario = 'ENTUSIASTA' | 'COLECIONADOR' | 'ORGANIZADOR_EVENTO' | 'PRESTADOR_SERVICO'
export type TipoPrestadorServico = 'MECANICO' | 'GUINCHO'
export type TipoServico = 'MECANICO' | 'GUINCHO'
export type StatusServico = 'SOLICITADO' | 'PROPOSTA' | 'PENDENTE' | 'CHEGOU' | 'FINALIZADO'
export type StatusEvento = 'ATIVO' | 'ESPERA' | 'INATIVO' | 'CANCELADO' | 'CONCLUIDO'
export type StatusInscricao = 'CONFIRMADA' | 'CANCELADA'
export type ServicosOfertados =
  | 'TROCA_DE_OLEO'
  | 'AJUSTE_DE_CARBURADOR'
  | 'TROCA_CABOS_VELA'
  | 'TROCA_FILTRO_AR'
  | 'FREIOS'
  | 'SUSPENSAO'

// Enums do carro (valores exatos — seção 6 do ComportamentoAtualBack.docx)
export type TipoCarroceria = 'HATCH' | 'SEDA' | 'SUV' | 'PICAPE' | 'CUPE' | 'PERUA' | 'CONVERSIVEL' | 'MINIVAN' | 'VAN'
export type TipoMotor = 'LINHA' | 'V' | 'BOXER' | 'WANKEL' | 'W' | 'RADIAL' | 'ELETRICO'
export type TipoInjecao = 'CARBURADO' | 'ELETRONICA' | 'PROGRAMAVEL'
export type TipoAlimentacao = 'ASPIRADO' | 'TURBO' | 'SUPERCHARGER' | 'PROCHARGER'
export type TipoCombustivel = 'GASOLINA' | 'ETANOL' | 'DIESEL' | 'FLEX' | 'GNV' | 'ELETRICO'
export type TipoCambio = 'MANUAL' | 'AUTOMATICO'
export type TipoTracao = 'TRASEIRA' | 'DIANTEIRA' | 'INTEGRAL' | 'QUATRO_X_QUATRO' | 'QUATRO_X_DOIS'
export type TipoSuspensao = 'FIXA' | 'ROSCA' | 'AR'
export type TipoFreio = 'DISCO' | 'TAMBOR'
export type TipoDirecao = 'MECANICA' | 'HIDRAULICA' | 'ELETRICA'
export type TipoRoda = 'FERRO' | 'LIGA_LEVE' | 'CARBONO'
export type TipoMaterialRevestimentoInterno =
  | 'TECIDO'
  | 'COURO_NATURAL'
  | 'COURO_SINTETICO'
  | 'ALCANTARA'
  | 'VELUDO'
  | 'PLASTICO_INJETADO'
  | 'FIBRA_DE_CARBONO'

// ---------- Envelope de paginação (PaginaRespostaDTO<T>) ----------
export interface PaginaRespostaDTO<T> {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
  ultima: boolean
}

export const ENVELOPE_VAZIO = <T>(): PaginaRespostaDTO<T> => ({
  conteudo: [],
  pagina: 0,
  tamanho: 5,
  totalElementos: 0,
  totalPaginas: 0,
  ultima: true,
})

// ---------- Autenticação ----------
export interface LoginResponseDTO {
  accessToken: string
  refreshToken: string
  tipoToken: string // "Bearer"
  expiraEmSegundos: number // 900 = 15 min
  idUsuario: string
  nomeUsuario: string
  tipoUsuario: TipoUsuario
  tipoPrestadorServico: TipoPrestadorServico | null
}

export interface LoginDTO {
  login: string
  senha: string
  tipoUsuarioSelecionado: TipoUsuario
  tipoPrestador?: TipoPrestadorServico | null
}

export interface CadastroDTO {
  nome: string
  login: string
  senha: string
  dataNascimento: string // LocalDate yyyy-MM-dd
  telefone: string
  tipoUsuario: TipoUsuario
  tipoPrestadorServico: TipoPrestadorServico | null
}

export interface CadastroIndividualDTO {
  email: string
  cpf?: string | null
  cnpj?: string | null
  placa?: string | null
  servicosOfertados?: ServicosOfertados[] | null
}

export interface VerificacaoRegistroRequestDTO {
  email: string
  codigo: string
}

export interface ReenvioCodigoDTO {
  email: string
}

export interface SolicitacaoCodigoSenhaDTO {
  email: string
}

export interface RedefinicaoSenhaDTO {
  email: string
  codigo: string
  novaSenha: string
}

export interface AlteracaoSenhaDTO {
  codigo: string
  novaSenha: string
}

export interface RenovacaoTokenDTO {
  refreshToken: string
}

// ---------- Perfis ----------
export interface PerfilUsuarioDTO {
  nome: string
  telefone: string
  login: string
  urlFotoPerfil: string | null
  notaMediaEntusiasta: number | null
  quantidadeAvaliacoes: number | null
  /** Localização salva no perfil (null = nenhuma; sempre null para o organizador). */
  latitude: number | null
  longitude: number | null
  /** Endereço salvo junto com as coordenadas (só entusiasta/colecionador; demais = null). */
  enderecoFormatado: string | null
}

export interface PerfilEntusiastaPutDTO {
  nome: string
  telefone: string
}
export interface PerfilEntusiastaPatchDTO {
  nome?: string
  telefone?: string
}
export interface PerfilOrganizadorEventoPutDTO {
  nome: string
  telefone: string
}
export interface PerfilOrganizadorEventoPatchDTO {
  nome?: string
  telefone?: string
}
export interface PerfilMecanicoPutDTO {
  nome: string
  telefone: string
  servicosOfertados: ServicosOfertados[]
}
export interface PerfilMecanicoPatchDTO {
  nome?: string
  telefone?: string
  servicosOfertados?: ServicosOfertados[]
}
export interface PerfilGuinchoPutDTO {
  nome: string
  telefone: string
  placa: string
}
export interface PerfilGuinchoPatchDTO {
  nome?: string
  telefone?: string
  placa?: string
}

export interface LatitudeLongitudeDTO {
  latitude?: number | null
  longitude?: number | null
}

// ---------- Carros ----------
export interface CarroRegistroDTO {
  marca: string
  modelo: string
  carroceria: TipoCarroceria
  ano: number
  cor: string
  quantidadeCilindros: number
  motor: TipoMotor
  litragemMotor: number
  injecao: TipoInjecao
  alimentacao: TipoAlimentacao
  combustivel: TipoCombustivel
  cambio: TipoCambio
  tracao: TipoTracao
  suspensao: TipoSuspensao
  freio: TipoFreio
  direcao: TipoDirecao
  roda: TipoRoda
  aro: number
  numeroPortas: number
  numeroAssentos: number
  materialRevestimentoInterno: TipoMaterialRevestimentoInterno
  placa: string
  vidrosEletricos: boolean
  som: boolean
  arCondicionado: boolean
  tetoSolar: boolean
  urlFotoCarro?: string[] | null // NÃO é mais obrigatório; fotos entram via multipart
}

export interface CarroPatchDTO {
  marca?: string
  modelo?: string
  carroceria?: TipoCarroceria
  ano?: number
  cor?: string
  quantidadeCilindros?: number
  motor?: TipoMotor
  litragemMotor?: number
  injecao?: TipoInjecao
  alimentacao?: TipoAlimentacao
  combustivel?: TipoCombustivel
  cambio?: TipoCambio
  tracao?: TipoTracao
  suspensao?: TipoSuspensao
  freio?: TipoFreio
  direcao?: TipoDirecao
  roda?: TipoRoda
  aro?: number
  numeroPortas?: number
  numeroAssentos?: number
  materialRevestimentoInterno?: TipoMaterialRevestimentoInterno
  placa?: string
  vidrosEletricos?: boolean
  som?: boolean
  arCondicionado?: boolean
  tetoSolar?: boolean
}

export interface CarroRespostaDTO {
  idCarro: string
  marca: string
  modelo: string
  placa: string
  nomeProprietario: string
  carroceria: TipoCarroceria
  ano: number
  cor: string
  quantidadeCilindros: number
  motor: TipoMotor
  litragemMotor: number
  injecao: TipoInjecao
  alimentacao: TipoAlimentacao
  combustivel: TipoCombustivel
  cambio: TipoCambio
  tracao: TipoTracao
  suspensao: TipoSuspensao
  freio: TipoFreio
  direcao: TipoDirecao
  roda: TipoRoda
  aro: number
  numeroPortas: number
  numeroAssentos: number
  materialRevestimentoInterno: TipoMaterialRevestimentoInterno
  vidrosEletricos: boolean
  som: boolean
  arCondicionado: boolean
  tetoSolar: boolean
  fotosCarro: string[]
  /** CLUBES: clubes de que o carro foi desvinculado nesta edição (deixou de atender ao gosto). */
  clubesDesvinculados: string[]
}

// ---------- Eventos ----------
export interface EventoRegistroDTO {
  nome: string
  descricao: string
  preco: number
  limiteParticipantes: number
  rua: string
  numero: string
  complemento?: string | null
  bairro: string
  cidade: string
  uf: string
  cep: string
  dataHoraInicio: string // LocalDateTime ISO sem offset
  dataHoraFim: string
  urlFotoEvento?: string[] | null
  /** Carros aceitos no evento. Ausente/vazio = aberto a todos. */
  filtroCarro?: FiltroCarroDTO | null
}

export interface EventoPutDTO extends EventoRegistroDTO {}

export interface EventoPatchDTO {
  nome?: string
  descricao?: string
  preco?: number
  limiteParticipantes?: number
  rua?: string
  numero?: string
  complemento?: string | null
  bairro?: string
  cidade?: string
  uf?: string
  cep?: string
  dataHoraInicio?: string
  dataHoraFim?: string
  urlFotoEvento?: string[] | null
  /** Presente = substitui o filtro (vazio = aberto a todos); ausente = mantém. */
  filtroCarro?: FiltroCarroDTO | null
}

export interface EventoRespostaDTO {
  idEvento: string
  nome: string
  preco: number
  descricao: string
  limiteParticipantes: number
  rua: string
  numero: string
  complemento: string | null
  bairro: string
  cidade: string
  uf: string
  cep: string
  latitude: number | null
  longitude: number | null
  status: StatusEvento
  nomeOrganizador: string
  dataHoraInicio: string
  dataHoraFim: string
  urlFotoEvento: string[]
  /** null = evento aberto a todos os carros. */
  filtroCarro: FiltroCarroDTO | null
}

export interface CatalogoEventoResponseDTO {
  idEvento: string
  nome: string
  descricao: string
  preco: number
  rua: string
  numero: string
  bairro: string
  cidade: string
  uf: string
  cep: string
  latitude: number | null
  longitude: number | null
  nomeOrganizador: string
  emailOrganizador: string
  vagasDisponiveis: number
  inscrito: boolean
  dataHoraInicio: string
  dataHoraFim: string
  fotos: string[]
}

// ---------- Inscrições ----------
export interface InscricaoRegistroDTO {
  idCarro?: string | null
}

export interface InscricaoResponseDTO {
  idInscricao: string
  nomeEvento: string
  nomeEntusiasta: string
  carroVinculado: string // "marca modelo (PLACA)" ou "Sem veículo vinculado"
  status: StatusInscricao
  dataInscricao: string
}

// ---------- Dashboard do organizador ----------
export interface DashboardMetricasResponseDTO {
  totalEventosCriados: number
  totalParticipantesMobilizados: number
}

export interface HistoricoEventoResponseDTO {
  idEvento: string
  nome: string
  status: StatusEvento
  limiteVagas: number
  totalParticipantes: number
}

// ---------- Serviços ----------
export interface ServicoMecanicoRegistroDTO {
  idCarro: string
  servicosOfertados: ServicosOfertados[]
  latitude?: number | null
  longitude?: number | null
  endereco?: string | null
}

export interface ServicoGuinchoRegistroDTO {
  idCarro: string
  latitude?: number | null
  longitude?: number | null
  endereco?: string | null
}

export interface PropostaServicoDTO {
  valorProposto: number
  latitude?: number | null
  longitude?: number | null
}

export interface AvaliacaoServicoDTO {
  nota: number
  comentario?: string | null
}

export interface EnderecoOcorrenciaDTO {
  rua: string | null
  numero: string | null
  bairro: string | null
  cidade: string | null
  estado: string | null
  cep: string | null
  placeId: string | null
}

export interface ServicoRespostaDTO {
  idServico: string
  tipoServico: TipoServico
  status: StatusServico
  valorProposto: number | null

  idEntusiasta: string
  nomeEntusiasta: string
  telefoneEntusiasta: string | null

  idPrestador: string | null
  nomePrestador: string | null
  telefonePrestador: string | null

  idCarro: string
  carroDescricao: string
  servicosSolicitados: ServicosOfertados[]
  quantidadeMecanicosElegiveis: number | null

  latitudeEntusiasta: number
  longitudeEntusiasta: number
  enderecoOcorrencia: string | null
  enderecoEstruturado: EnderecoOcorrenciaDTO | null

  latitudePrestador: number | null
  longitudePrestador: number | null

  urlFotoInicial: string | null

  confirmacaoChegadaEntusiasta: boolean
  confirmacaoChegadaPrestador: boolean
  dataHoraChegou: string | null

  urlFotoFinalEntusiasta: string | null
  urlFotoFinalPrestador: string | null

  notaAvaliacaoEntusiasta: number | null
  comentarioAvaliacaoEntusiasta: string | null
  dataHoraAvaliacaoEntusiasta: string | null

  notaAvaliacaoPrestador: number | null
  comentarioAvaliacaoPrestador: string | null
  dataHoraAvaliacaoPrestador: string | null

  confirmacaoConclusaoEntusiasta: boolean
  confirmacaoConclusaoPrestador: boolean

  dataHoraSolicitacao: string
  dataHoraProposta: string | null
  dataHoraPendente: string | null
  dataHoraFinalizacao: string | null

  notaMatch: number | null
  notaMediaPrestador: number | null
  quantidadeAvaliacoes: number | null
  distanciaAteOcorrencia: number | null
  servicosAtivosDoPrestador: number | null
}

export interface ServicoDisponivelDTO {
  idServico: string
  nomeEntusiasta: string
  distanciaKm: number
  servicosSolicitados: ServicosOfertados[]
  carroDescricao: string
  notaMatch: number | null
}

export interface PrestadorCompativelDTO {
  idPrestador: string
  tipoPrestador: TipoPrestadorServico
  nomePrestador: string
  notaAjustada: number
  quantidadeAvaliacoes: number
  distanciaKm: number
  notaMatch: number
}

export interface HistoricoManutencaoResponseDTO {
  idServico: string
  tipoServico: TipoServico
  nomePrestador: string
  servicosRealizados: ServicosOfertados[]
  enderecoOcorrencia: string | null
  valorProposto: number
  dataHoraFinalizacao: string
}

// ---------- Clubes ----------
/**
 * Gosto do clube / carros aceitos no evento. Campo ausente, lista vazia ou faixa
 * sem pontas = critério não marcado (qualquer valor passa).
 */
export interface FiltroCarroDTO {
  marcas?: string[] | null
  modelos?: string[] | null
  cores?: string[] | null
  carrocerias?: TipoCarroceria[] | null
  motores?: TipoMotor[] | null
  injecoes?: TipoInjecao[] | null
  alimentacoes?: TipoAlimentacao[] | null
  combustiveis?: TipoCombustivel[] | null
  cambios?: TipoCambio[] | null
  tracoes?: TipoTracao[] | null
  suspensoes?: TipoSuspensao[] | null
  freios?: TipoFreio[] | null
  direcoes?: TipoDirecao[] | null
  rodas?: TipoRoda[] | null
  materiaisRevestimentoInterno?: TipoMaterialRevestimentoInterno[] | null
  anoMin?: number | null
  anoMax?: number | null
  quantidadeCilindrosMin?: number | null
  quantidadeCilindrosMax?: number | null
  litragemMotorMin?: number | null
  litragemMotorMax?: number | null
  aroMin?: number | null
  aroMax?: number | null
  numeroPortasMin?: number | null
  numeroPortasMax?: number | null
  numeroAssentosMin?: number | null
  numeroAssentosMax?: number | null
  /** true = exige som; false/null = não é critério. */
  som?: boolean | null
}

export interface ClubeRegistroDTO {
  nome: string
  descricao?: string | null
  filtro: FiltroCarroDTO
  /** PUT sem arquivos: mantém estas URLs. */
  fotosClube?: string[] | null
}

export interface ClubeRespostaDTO {
  idClube: string
  nome: string
  descricao: string | null
  idAdministrador: string
  nomeAdministrador: string
  quantidadeMembros: number
  quantidadeCarrosVinculados: number
  souMembro: boolean
  souAdministrador: boolean
  filtro: FiltroCarroDTO
  fotosClube: string[]
  dataCriacao: string
  /** Só na resposta da edição: carros desvinculados pelo novo filtro. */
  carrosDesvinculados: number
}

export interface CarroVinculadoResumoDTO {
  idCarro: string
  marca: string
  modelo: string
  ano: number
  cor: string
  fotoCapa: string | null
}

export interface MembroClubeRespostaDTO {
  idEntusiasta: string
  nome: string
  urlFotoPerfil: string | null
  administrador: boolean
  dataEntrada: string
  /** Vazio para o entusiasta sem garagem. */
  carrosVinculados: CarroVinculadoResumoDTO[]
}

export interface CarroCompativelDTO {
  idCarro: string
  marca: string
  modelo: string
  ano: number
  cor: string
  fotoCapa: string | null
  vinculado: boolean
}

export interface EntradaClubeDTO {
  idsCarros?: string[] | null
}
