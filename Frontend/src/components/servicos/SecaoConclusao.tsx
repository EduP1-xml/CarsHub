import { Alerta } from '../Alerta'
import { Botao } from '../Botao'
import type { ServicoRespostaDTO } from '../../types/api'

interface SecaoConclusaoProps {
  servico: ServicoRespostaDTO
  /** 'entusiasta' (colecionador) ou 'prestador'. */
  lado: 'entusiasta' | 'prestador'
  rotuloOutro: string
  aoConfirmar: () => Promise<void>
  confirmando: boolean
  mensagem: string | null
}

/**
 * F23/F43 — conclusão com dupla confirmação: exige CHEGOU + a PRÓPRIA foto final +
 * a PRÓPRIA avaliação; as duas confirmações → FINALIZADO.
 */
export function SecaoConclusao({ servico, lado, rotuloOutro, aoConfirmar, confirmando, mensagem }: SecaoConclusaoProps) {
  const propriaFoto = lado === 'entusiasta' ? servico.urlFotoFinalEntusiasta : servico.urlFotoFinalPrestador
  const propriaNota = lado === 'entusiasta' ? servico.notaAvaliacaoEntusiasta : servico.notaAvaliacaoPrestador
  const propriaConfirmacao =
    lado === 'entusiasta' ? servico.confirmacaoConclusaoEntusiasta : servico.confirmacaoConclusaoPrestador
  const outraConfirmacao =
    lado === 'entusiasta' ? servico.confirmacaoConclusaoPrestador : servico.confirmacaoConclusaoEntusiasta

  const prerequisitos = !propriaFoto || propriaNota === null

  return (
    <div className="detalhe-secao">
      <h2>Conclusão do serviço</h2>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <ul className="checklist">
        <li className={propriaFoto ? 'ok' : 'pendente'}>
          {propriaFoto ? '✓' : '○'} Sua foto final enviada
        </li>
        <li className={propriaNota !== null ? 'ok' : 'pendente'}>
          {propriaNota !== null ? '✓' : '○'} Sua avaliação enviada
        </li>
        <li className={propriaConfirmacao ? 'ok' : 'pendente'}>
          {propriaConfirmacao ? '✓' : '○'} Você confirmou a conclusão
        </li>
        <li className={outraConfirmacao ? 'ok' : 'pendente'}>
          {outraConfirmacao ? '✓' : '○'} {rotuloOutro} confirmou a conclusão
        </li>
      </ul>

      {propriaConfirmacao ? (
        <p className="abreviado">Aguardando a confirmação do {rotuloOutro}...</p>
      ) : (
        <Botao carregando={confirmando} disabled={prerequisitos} onClick={() => void aoConfirmar()}>
          Confirmar conclusão
        </Botao>
      )}
      {prerequisitos && !propriaConfirmacao && (
        <p className="abreviado">Você precisa enviar a sua foto final e a sua avaliação antes de confirmar.</p>
      )}
    </div>
  )
}
