import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { LoadingPagina } from '../../components/Loading'
import { BadgeNotaMatch, BadgeStatusServico, BadgeTipoServico } from '../../components/BadgeStatus'
import { SecaoAvaliacao } from '../../components/servicos/SecaoAvaliacao'
import { SecaoChegada } from '../../components/servicos/SecaoChegada'
import { SecaoConclusao } from '../../components/servicos/SecaoConclusao'
import { SecaoFotoFinal } from '../../components/servicos/SecaoFotoFinal'
import {
  avaliarColecionador,
  buscarChamadoPrestador,
  confirmarChegadaPrestador,
  confirmarConclusaoPrestador,
  enviarFotoFinalPrestador,
  type PrefixoChamado,
} from '../../api/servicos'
import { useToast } from '../../context/ToastContext'
import type { ServicoRespostaDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDataHora, formatarDistancia, formatarMoeda, formatarNota } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F38–F43 — detalhe do chamado pelo lado do PRESTADOR (mecânico/guincho):
 * PROPOSTA = aguardando decisão; PENDENTE = confirmar chegada (dupla);
 * CHEGOU = foto final + avaliar o colecionador + confirmar conclusão (dupla).
 */
export function ServicoDetalhePrestador({ prefixo }: { prefixo: PrefixoChamado }) {
  const { idServico = '' } = useParams()
  const { toast } = useToast()

  const [servico, setServico] = useState<ServicoRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<unknown | null>(null)
  const [acaoEmAndamento, setAcaoEmAndamento] = useState<string | null>(null)
  const [mensagemAcao, setMensagemAcao] = useState<string | null>(null)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      const resposta = await buscarChamadoPrestador(prefixo, idServico)
      setServico(resposta.data)
    } catch (e) {
      setErro(e)
    } finally {
      setCarregando(false)
    }
  }, [prefixo, idServico])

  useEffect(() => {
    void carregar()
  }, [carregar])

  const executar = useCallback(
    async (nome: string, acao: () => Promise<{ data: ServicoRespostaDTO }>, mensagemSucesso: string) => {
      setMensagemAcao(null)
      setAcaoEmAndamento(nome)
      try {
        const resposta = await acao()
        setServico(resposta.data)
        toast('sucesso', mensagemSucesso)
      } catch (e) {
        setMensagemAcao(mensagemDoErro(e))
        await carregar() // 422 de estado/concorrência → refetch (R1)
      } finally {
        setAcaoEmAndamento(null)
      }
    },
    [carregar, toast],
  )

  if (carregando && !servico) return <LoadingPagina rotulo="Carregando chamado..." />

  if (erro || !servico) {
    return (
      <div>
        <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>
        <Link to="/meus-servicos" className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar para a fila
        </Link>
      </div>
    )
  }

  const s = servico
  const rotuloColecionador = 'colecionador'

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{s.carroDescricao}</h1>
        <div className="botoes-inline">
          <BadgeTipoServico tipo={s.tipoServico} />
          <BadgeStatusServico status={s.status} />
        </div>
      </div>

      {mensagemAcao && <Alerta tipo="erro">{mensagemAcao}</Alerta>}

      <div className="detalhe-secao">
        <h2>Informações</h2>
        <dl className="linha-tempo">
          <dt>Colecionador</dt>
          <dd>{s.nomeEntusiasta}</dd>
          {s.servicosSolicitados.length > 0 && (
            <>
              <dt>Serviços solicitados</dt>
              <dd>
                <span className="chips">
                  {s.servicosSolicitados.map((serv) => (
                    <span key={serv} className="chip">
                      {LABEL_SERVICOS_OFERTADOS[serv]}
                    </span>
                  ))}
                </span>
              </dd>
            </>
          )}
          <dt>Endereço da ocorrência</dt>
          <dd>{s.enderecoOcorrencia ?? '—'}</dd>
          <dt>Distância até a ocorrência</dt>
          <dd>{formatarDistancia(s.distanciaAteOcorrencia)}</dd>
          {s.valorProposto !== null && (
            <>
              <dt>Valor proposto</dt>
              <dd>
                <strong>{formatarMoeda(s.valorProposto)}</strong>
              </dd>
            </>
          )}
          <dt>notaMatch (informacional)</dt>
          <dd>
            <BadgeNotaMatch notaMatch={s.notaMatch} />
          </dd>
          <dt>Sua nota média (ajustada)</dt>
          <dd>{formatarNota(s.notaMediaPrestador)}</dd>
          <dt>Solicitado em</dt>
          <dd>{formatarDataHora(s.dataHoraSolicitacao)}</dd>
          {s.dataHoraPendente && (
            <>
              <dt>Aceito em</dt>
              <dd>{formatarDataHora(s.dataHoraPendente)}</dd>
            </>
          )}
          {s.dataHoraChegou && (
            <>
              <dt>Chegada em</dt>
              <dd>{formatarDataHora(s.dataHoraChegou)}</dd>
            </>
          )}
          {s.dataHoraFinalizacao && (
            <>
              <dt>Finalizado em</dt>
              <dd>{formatarDataHora(s.dataHoraFinalizacao)}</dd>
            </>
          )}
        </dl>

        {/* Telefones: null até PENDENTE (R8). */}
        {(s.status === 'PENDENTE' || s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
          <>
            {s.telefoneEntusiasta && (
              <div className="linha-dados">
                <span className="rotulo">Telefone do colecionador</span>
                <span>{s.telefoneEntusiasta}</span>
              </div>
            )}
            {s.telefonePrestador && (
              <div className="linha-dados">
                <span className="rotulo">Seu telefone</span>
                <span>{s.telefonePrestador}</span>
              </div>
            )}
          </>
        )}
      </div>

      {s.status === 'PROPOSTA' && (
        <div className="detalhe-secao">
          <h2>Proposta enviada</h2>
          <p>
            Sua proposta de <strong>{formatarMoeda(s.valorProposto)}</strong> está aguardando a decisão do
            colecionador. Enquanto isso, o chamado está fora da rede.
          </p>
        </div>
      )}

      {s.status === 'PENDENTE' && (
        <SecaoChegada
          servico={s}
          lado="prestador"
          confirmacaoPropria={s.confirmacaoChegadaPrestador}
          confirmacaoOutro={s.confirmacaoChegadaEntusiasta}
          rotuloOutro={rotuloColecionador}
          confirmando={acaoEmAndamento === 'chegada'}
          mensagem={null}
          aoConfirmar={() =>
            executar('chegada', () => confirmarChegadaPrestador(prefixo, idServico), 'Chegada confirmada por você.')
          }
        />
      )}

      {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
        <SecaoFotoFinal
          servico={s}
          lado="prestador"
          rotuloOutro={rotuloColecionador}
          enviando={acaoEmAndamento === 'foto-final'}
          mensagem={null}
          aoEnviar={(arquivo) =>
            executar('foto-final', () => enviarFotoFinalPrestador(prefixo, idServico, arquivo), 'Foto final enviada.')
          }
        />
      )}

      {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
        <SecaoAvaliacao
          servico={s}
          lado="prestador"
          rotuloAlvo={rotuloColecionador}
          enviando={acaoEmAndamento === 'avaliacao'}
          mensagem={null}
          aoEnviar={(nota, comentario) =>
            executar(
              'avaliacao',
              () => avaliarColecionador(prefixo, idServico, { nota, comentario }),
              'Avaliação enviada. Ela é imutável.',
            )
          }
        />
      )}

      {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
        <SecaoConclusao
          servico={s}
          lado="prestador"
          rotuloOutro={rotuloColecionador}
          confirmando={acaoEmAndamento === 'conclusao'}
          mensagem={null}
          aoConfirmar={() =>
            executar('conclusao', () => confirmarConclusaoPrestador(prefixo, idServico), 'Conclusão confirmada por você.')
          }
        />
      )}

      {s.status === 'PENDENTE' && !s.urlFotoInicial && (
        <p className="abreviado" style={{ marginTop: 8 }}>
          <Botao variante="texto" onClick={() => void carregar()}>
            Atualizar
          </Botao>{' '}
          — aguardando o colecionador enviar a foto inicial para confirmar a chegada.
        </p>
      )}

      {s.status !== 'FINALIZADO' && (
        <div className="form-acoes">
          <Botao variante="texto" onClick={() => void carregar()}>
            ↻ Atualizar dados
          </Botao>
        </div>
      )}
    </div>
  )
}
