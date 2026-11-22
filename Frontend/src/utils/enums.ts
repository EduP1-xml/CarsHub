// ============================================================================
// "Labels humanizados" dos enums (termo do guia): texto legível para exibição.
// O JSON enviado DEVE conter SEMPRE o valor do enum — estes mapas são só display.
// ============================================================================
import type {
  ServicosOfertados,
  StatusEvento,
  StatusInscricao,
  StatusServico,
  TipoAlimentacao,
  TipoCambio,
  TipoCarroceria,
  TipoCombustivel,
  TipoDirecao,
  TipoFreio,
  TipoInjecao,
  TipoMaterialRevestimentoInterno,
  TipoMotor,
  TipoPrestadorServico,
  TipoRoda,
  TipoServico,
  TipoSuspensao,
  TipoTracao,
  TipoUsuario,
} from '../types/api'

export const LABEL_TIPO_USUARIO: Record<TipoUsuario, string> = {
  ENTUSIASTA: 'Entusiasta',
  COLECIONADOR: 'Colecionador',
  ORGANIZADOR_EVENTO: 'Organizador de eventos',
  PRESTADOR_SERVICO: 'Prestador de serviços',
}

export const LABEL_TIPO_PRESTADOR: Record<TipoPrestadorServico, string> = {
  MECANICO: 'Mecânico',
  GUINCHO: 'Guincho',
}

export const LABEL_TIPO_SERVICO: Record<TipoServico, string> = {
  MECANICO: 'Serviço mecânico',
  GUINCHO: 'Serviço de guincho',
}

export const LABEL_STATUS_SERVICO: Record<StatusServico, string> = {
  SOLICITADO: 'Solicitado',
  PROPOSTA: 'Proposta recebida',
  PENDENTE: 'Em andamento (PENDENTE)',
  CHEGOU: 'Prestador no local (CHEGOU)',
  FINALIZADO: 'Finalizado',
}

export const LABEL_STATUS_EVENTO: Record<StatusEvento, string> = {
  ATIVO: 'Ativo',
  ESPERA: 'Em espera',
  INATIVO: 'Inativo',
  CANCELADO: 'Cancelado',
  CONCLUIDO: 'Concluído',
}

export const LABEL_STATUS_INSCRICAO: Record<StatusInscricao, string> = {
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
}

export const LABEL_SERVICOS_OFERTADOS: Record<ServicosOfertados, string> = {
  TROCA_DE_OLEO: 'Troca de óleo',
  AJUSTE_DE_CARBURADOR: 'Ajuste de carburador',
  TROCA_CABOS_VELA: 'Troca de cabos de vela',
  TROCA_FILTRO_AR: 'Troca de filtro de ar',
  FREIOS: 'Freios',
  SUSPENSAO: 'Suspensão',
}

export const LABEL_CARROCERIA: Record<TipoCarroceria, string> = {
  HATCH: 'Hatch', SEDA: 'Sedã', SUV: 'SUV', PICAPE: 'Picape', CUPE: 'Cupê',
  PERUA: 'Perua', CONVERSIVEL: 'Conversível', MINIVAN: 'Minivan', VAN: 'Van',
}

export const LABEL_MOTOR: Record<TipoMotor, string> = {
  LINHA: 'Linha', V: 'V', BOXER: 'Boxer', WANKEL: 'Wankel', W: 'W', RADIAL: 'Radial', ELETRICO: 'Elétrico',
}

export const LABEL_INJECAO: Record<TipoInjecao, string> = {
  CARBURADO: 'Carburado', ELETRONICA: 'Eletrônica', PROGRAMAVEL: 'Programável',
}

export const LABEL_ALIMENTACAO: Record<TipoAlimentacao, string> = {
  ASPIRADO: 'Aspirado', TURBO: 'Turbo', SUPERCHARGER: 'Supercharger', PROCHARGER: 'Procharger',
}

export const LABEL_COMBUSTIVEL: Record<TipoCombustivel, string> = {
  GASOLINA: 'Gasolina', ETANOL: 'Etanol', DIESEL: 'Diesel', FLEX: 'Flex', GNV: 'GNV', ELETRICO: 'Elétrico',
}

export const LABEL_CAMBIO: Record<TipoCambio, string> = {
  MANUAL: 'Manual', AUTOMATICO: 'Automático',
}

export const LABEL_TRACAO: Record<TipoTracao, string> = {
  TRASEIRA: 'Traseira', DIANTEIRA: 'Dianteira', INTEGRAL: 'Integral', QUATRO_X_QUATRO: '4x4', QUATRO_X_DOIS: '4x2',
}

export const LABEL_SUSPENSAO: Record<TipoSuspensao, string> = {
  FIXA: 'Fixa', ROSCA: 'Rosca', AR: 'A ar',
}

export const LABEL_FREIO: Record<TipoFreio, string> = {
  DISCO: 'Disco', TAMBOR: 'Tambor',
}

export const LABEL_DIRECAO: Record<TipoDirecao, string> = {
  MECANICA: 'Mecânica', HIDRAULICA: 'Hidráulica', ELETRICA: 'Elétrica',
}

export const LABEL_RODA: Record<TipoRoda, string> = {
  FERRO: 'Ferro', LIGA_LEVE: 'Liga leve', CARBONO: 'Carbono',
}

export const LABEL_REVESTIMENTO: Record<TipoMaterialRevestimentoInterno, string> = {
  TECIDO: 'Tecido', COURO_NATURAL: 'Couro natural', COURO_SINTETICO: 'Couro sintético',
  ALCANTARA: 'Alcântara', VELUDO: 'Veludo', PLASTICO_INJETADO: 'Plástico injetado', FIBRA_DE_CARBONO: 'Fibra de carbono',
}

export const OPCOES_SERVICOS_OFERTADOS: ServicosOfertados[] = [
  'TROCA_DE_OLEO', 'AJUSTE_DE_CARBURADOR', 'TROCA_CABOS_VELA', 'TROCA_FILTRO_AR', 'FREIOS', 'SUSPENSAO',
]
