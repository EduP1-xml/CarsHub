import { Alerta } from '../Alerta'
import { Botao } from '../Botao'
import type { ServicoRespostaDTO } from '../../types/api'
import { formatarDataHora } from '../../utils/formatadores'

interface SecaoChegadaProps {
  servico: ServicoRespostaDTO
  /** 'entusiasta' (colecionador) ou 'prestador'. */
  lado: 'entusiasta' | 'prestador'
  confirmacaoPropria: boolean
  confirmacaoOutro: boolean
  /** Rótulo da outra parte ("mecânico", "guincho" ou "colecionador"). */
  rotuloOutro: string
  aoConfirmar: () => Promise<void>
  confirmando: boolean
  mensagem: string | null
}

/**
 * F20/F40 — confirmação de chegada (dupla confirmação): cada parte confirma;
 * quando AMBAS confirmam o status vira CHEGOU. Exige foto inicial do colecionador.
 */
export function SecaoChegada({
  servico,
  lado,
  confirmacaoPropria,
  confirmacaoOutro,
  rotuloOutro,
  aoConfirmar,
  confirmando,
  mensagem,
}: SecaoChegadaProps) {
  return (
    <div className="detalhe-secao">
      <h2>Chegada ao local</h2>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      {servico.status === 'CHEGOU' || servico.status === 'FINALIZADO' ? (
        <p>
          <strong>Prestador chegou ao local.</strong>
          {servico.dataHoraChegou && ` Data: ${formatarDataHora(servico.dataHoraChegou)}`}
        </p>
      ) : (
        <>
          <ul className="checklist">
            <li className={confirmacaoPropria ? 'ok' : 'pendente'}>
              {confirmacaoPropria ? '✓' : '○'} Você confirmou a chegada
            </li>
            <li className={confirmacaoOutro ? 'ok' : 'pendente'}>
              {confirmacaoOutro ? '✓' : '○'} {rotuloOutro} confirmou a chegada
            </li>
          </ul>
          {confirmacaoPropria ? (
            <p className="abreviado">Aguardando a confirmação do {rotuloOutro}...</p>
          ) : (
            <Botao carregando={confirmando} onClick={() => void aoConfirmar()}>
              Confirmar chegada
            </Botao>
          )}
        </>
      )}
      {lado === 'prestador' && !servico.urlFotoInicial && (
        <p className="abreviado">O colecionador precisa enviar a foto inicial antes da confirmação de chegada.</p>
      )}
    </div>
  )
}
