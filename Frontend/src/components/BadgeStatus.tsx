import {
  LABEL_STATUS_EVENTO,
  LABEL_STATUS_INSCRICAO,
  LABEL_STATUS_SERVICO,
  LABEL_TIPO_SERVICO,
} from '../utils/enums'
import type { StatusEvento, StatusInscricao, StatusServico, TipoServico } from '../types/api'

const CLASSE_STATUS_SERVICO: Record<StatusServico, string> = {
  SOLICITADO: 'badge-azul',
  PROPOSTA: 'badge-amarelo',
  PENDENTE: 'badge-roxo',
  CHEGOU: 'badge-laranja',
  FINALIZADO: 'badge-verde',
}

const CLASSE_STATUS_EVENTO: Record<StatusEvento, string> = {
  ATIVO: 'badge-verde',
  ESPERA: 'badge-amarelo',
  INATIVO: 'badge-cinza',
  CANCELADO: 'badge-vermelho',
  CONCLUIDO: 'badge-azul',
}

export function BadgeStatusServico({ status }: { status: StatusServico }) {
  return <span className={`badge ${CLASSE_STATUS_SERVICO[status]}`}>{LABEL_STATUS_SERVICO[status]}</span>
}

export function BadgeStatusEvento({ status }: { status: StatusEvento }) {
  return <span className={`badge ${CLASSE_STATUS_EVENTO[status]}`}>{LABEL_STATUS_EVENTO[status]}</span>
}

export function BadgeStatusInscricao({ status }: { status: StatusInscricao }) {
  return (
    <span className={`badge ${status === 'CONFIRMADA' ? 'badge-verde' : 'badge-cinza'}`}>
      {LABEL_STATUS_INSCRICAO[status]}
    </span>
  )
}

export function BadgeTipoServico({ tipo }: { tipo: TipoServico }) {
  return <span className={`badge ${tipo === 'MECANICO' ? 'badge-azul' : 'badge-roxo'}`}>{LABEL_TIPO_SERVICO[tipo]}</span>
}

/** notaMatch é informacional: exibir como badge 0..100; NUNCA recalcular no front (R11). */
export function BadgeNotaMatch({ notaMatch }: { notaMatch: number | null }) {
  if (notaMatch === null) return <span className="badge badge-cinza">match —</span>
  return <span className="badge badge-azul">match {notaMatch}</span>
}
