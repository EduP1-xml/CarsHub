import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import {
  BadgeNotaMatch,
  BadgeStatusServico,
  BadgeTipoServico,
} from '../../components/BadgeStatus'
import { SecaoAvaliacao } from '../../components/servicos/SecaoAvaliacao'
import { SecaoChegada } from '../../components/servicos/SecaoChegada'
import { SecaoConclusao } from '../../components/servicos/SecaoConclusao'
import { SecaoFotoFinal } from '../../components/servicos/SecaoFotoFinal'
import { SecaoFotoInicial } from '../../components/servicos/SecaoFotoInicial'
import {
  aceitarProposta,
  avaliarPrestador,
  buscarMeuChamado,
  cancelarChamado,
  confirmarChegadaColecionador,
  confirmarConclusaoColecionador,
  enviarFotoFinalColecionador,
  enviarFotoInicial,
  listarPrestadoresCompativeis,
  rejeitarProposta,
  type PrefixoChamado,
} from '../../api/servicos'
import { usePaginacao } from '../../hooks/usePaginacao'
import { useToast } from '../../context/ToastContext'
import type { PrestadorCompativelDTO, ServicoRespostaDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS, LABEL_TIPO_PRESTADOR } from '../../utils/enums'
import {
  formatarDataHora,
  formatarDistancia,
  formatarMoeda,
  formatarNota,
  minutosRestantesCancelamento,
} from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F17–F26 — detalhe do chamado (colecionador) com ações dirigidas pelo status +
 * aba do ranking de prestadores compatíveis (ordem vem pronta — não reordenar).
 */
export function ServicoDetalhe() {
  const { prefixo = 'mecanicos', idServico = '' } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()

  const [servico, setServico] = useState<ServicoRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<unknown | null>(null)
  const [acaoEmAndamento, setAcaoEmAndamento] = useState<string | null>(null)
  const [mensagemAcao, setMensagemAcao] = useState<string | null>(null)
  const [confirmandoCancelar, setConfirmandoCancelar] = useState(false)
  const [aba, setAba] = useState<'detalhe' | 'ranking'>('detalhe')

  const prefixoTipado = prefixo as PrefixoChamado

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      const resposta = await buscarMeuChamado(prefixoTipado, idServico)
      setServico(resposta.data)
    } catch (e) {
      setErro(e)
    } finally {
      setCarregando(false)
    }
  }, [prefixoTipado, idServico])

  useEffect(() => {
    void carregar()
  }, [carregar])

  /** Runner genérico: executa a ação e recarrega; em 422 (concorrência/estado) faz refetch (R1). */
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
        // Estado mudou sozinho/tarefa ou concorrência: refetch do detalhe.
        await carregar()
      } finally {
        setAcaoEmAndamento(null)
      }
    },
    [carregar, toast],
  )

  async function aoCancelar() {
    setMensagemAcao(null)
    setAcaoEmAndamento('cancelar')
    try {
      await cancelarChamado(prefixoTipado, idServico)
      toast('sucesso', 'Solicitação cancelada.')
      navigate('/servicos')
    } catch (e) {
      setMensagemAcao(mensagemDoErro(e))
      setConfirmandoCancelar(false)
      await carregar()
    } finally {
      setAcaoEmAndamento(null)
    }
  }

  if (carregando && !servico) return <LoadingPagina rotulo="Carregando solicitação..." />

  if (erro || !servico) {
    return (
      <div>
        <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>
        <Link to="/servicos" className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar para meus serviços
        </Link>
      </div>
    )
  }

  const s = servico
  const rotuloPrestador = s.tipoServico === 'MECANICO' ? 'mecânico' : 'guincho'
  const minutosRestantes = minutosRestantesCancelamento(s.dataHoraPendente)
  const podeCancelar = s.status === 'SOLICITADO' || s.status === 'PROPOSTA' || (s.status === 'PENDENTE' && minutosRestantes === 0)

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{s.carroDescricao}</h1>
        <div className="botoes-inline">
          <BadgeTipoServico tipo={s.tipoServico} />
          <BadgeStatusServico status={s.status} />
        </div>
      </div>

      <div className="tabs">
        <button type="button" className={`tab${aba === 'detalhe' ? ' tab-ativa' : ''}`} onClick={() => setAba('detalhe')}>
          Detalhe
        </button>
        <button type="button" className={`tab${aba === 'ranking' ? ' tab-ativa' : ''}`} onClick={() => setAba('ranking')}>
          Prestadores compatíveis
        </button>
      </div>

      {aba === 'ranking' ? (
        <RankingPrestadores idServico={idServico} />
      ) : (
        <>
          {mensagemAcao && <Alerta tipo="erro">{mensagemAcao}</Alerta>}

          <div className="detalhe-secao">
            <h2>Informações</h2>
            <dl className="linha-tempo">
              <dt>Serviços solicitados</dt>
              <dd>
                {s.servicosSolicitados.length > 0 ? (
                  <span className="chips">
                    {s.servicosSolicitados.map((serv) => (
                      <span key={serv} className="chip">
                        {LABEL_SERVICOS_OFERTADOS[serv]}
                      </span>
                    ))}
                  </span>
                ) : (
                  '—'
                )}
              </dd>
              <dt>Endereço da ocorrência</dt>
              <dd>{s.enderecoOcorrencia ?? '—'}</dd>
              <dt>Localização</dt>
              <dd>
                {s.latitudeEntusiasta}, {s.longitudeEntusiasta}
              </dd>
              <dt>Solicitado em</dt>
              <dd>{formatarDataHora(s.dataHoraSolicitacao)}</dd>
              {s.dataHoraProposta && (
                <>
                  <dt>Proposta em</dt>
                  <dd>{formatarDataHora(s.dataHoraProposta)}</dd>
                </>
              )}
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
              {s.status === 'SOLICITADO' && s.tipoServico === 'MECANICO' && s.quantidadeMecanicosElegiveis !== null && (
                <>
                  <dt>Mecânicos compatíveis</dt>
                  <dd>
                    {s.quantidadeMecanicosElegiveis === 0
                      ? 'Nenhum mecânico compatível no momento.'
                      : s.quantidadeMecanicosElegiveis}
                  </dd>
                </>
              )}
            </dl>

            {/* Telefones: null até PENDENTE (R8) — esconder. */}
            {(s.status === 'PENDENTE' || s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
              <>
                {s.telefoneEntusiasta && (
                  <div className="linha-dados">
                    <span className="rotulo">Seu telefone</span>
                    <span>{s.telefoneEntusiasta}</span>
                  </div>
                )}
                {s.telefonePrestador && (
                  <div className="linha-dados">
                    <span className="rotulo">Telefone do {rotuloPrestador}</span>
                    <span>{s.telefonePrestador}</span>
                  </div>
                )}
              </>
            )}
          </div>

          {/* PROPOSTA: prestador vinculado + campos de match (null sem prestador) */}
          {s.status === 'PROPOSTA' && s.nomePrestador && (
            <div className="detalhe-secao">
              <h2>Proposta recebida</h2>
              <dl className="linha-tempo">
                <dt>Prestador</dt>
                <dd>{s.nomePrestador}</dd>
                <dt>Valor proposto</dt>
                <dd>
                  <strong>{formatarMoeda(s.valorProposto)}</strong>
                </dd>
                <dt>notaMatch (0..100)</dt>
                <dd>
                  <BadgeNotaMatch notaMatch={s.notaMatch} />
                </dd>
                <dt>Nota média (ajustada)</dt>
                <dd>{formatarNota(s.notaMediaPrestador)}</dd>
                <dt>Avaliações</dt>
                <dd>{s.quantidadeAvaliacoes ?? '—'}</dd>
                <dt>Distância até a ocorrência</dt>
                <dd>{formatarDistancia(s.distanciaAteOcorrencia)}</dd>
                <dt>Serviços ativos do prestador</dt>
                <dd>{s.servicosAtivosDoPrestador ?? '—'}</dd>
              </dl>
              <div className="form-acoes">
                <Botao
                  variante="sucesso"
                  carregando={acaoEmAndamento === 'aceitar'}
                  onClick={() =>
                    void executar('aceitar', () => aceitarProposta(prefixoTipado, idServico), 'Proposta aceita! O prestador está a caminho (PENDENTE).')
                  }
                >
                  Aceitar proposta
                </Botao>
                <Botao
                  variante="secundario"
                  carregando={acaoEmAndamento === 'rejeitar'}
                  onClick={() =>
                    void executar(
                      'rejeitar',
                      () => rejeitarProposta(prefixoTipado, idServico),
                      'Proposta rejeitada. O chamado voltou para a rede (SOLICITADO).',
                    )
                  }
                >
                  Rejeitar proposta
                </Botao>
              </div>
            </div>
          )}

          {/* PENDENTE: foto inicial + chegada + cancelamento (regra das 2 h) */}
          {(s.status === 'PENDENTE' || s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
            <SecaoFotoInicial
              servico={s}
              enviando={acaoEmAndamento === 'foto-inicial'}
              mensagem={null}
              aoEnviar={async (arquivo) => {
                await executar(
                  'foto-inicial',
                  () => enviarFotoInicial(prefixoTipado, idServico, arquivo),
                  'Foto inicial enviada. Ela é imutável.',
                )
              }}
            />
          )}

          {s.status === 'PENDENTE' && (
            <SecaoChegada
              servico={s}
              lado="entusiasta"
              confirmacaoPropria={s.confirmacaoChegadaEntusiasta}
              confirmacaoOutro={s.confirmacaoChegadaPrestador}
              rotuloOutro={rotuloPrestador}
              confirmando={acaoEmAndamento === 'chegada'}
              mensagem={null}
              aoConfirmar={() =>
                executar('chegada', () => confirmarChegadaColecionador(prefixoTipado, idServico), 'Chegada confirmada por você.')
              }
            />
          )}

          {/* CHEGOU: foto final + avaliação + conclusão */}
          {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
            <SecaoFotoFinal
              servico={s}
              lado="entusiasta"
              rotuloOutro={rotuloPrestador}
              enviando={acaoEmAndamento === 'foto-final'}
              mensagem={null}
              aoEnviar={(arquivo) =>
                executar('foto-final', () => enviarFotoFinalColecionador(prefixoTipado, idServico, arquivo), 'Foto final enviada.')
              }
            />
          )}

          {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
            <SecaoAvaliacao
              servico={s}
              lado="entusiasta"
              rotuloAlvo={rotuloPrestador}
              enviando={acaoEmAndamento === 'avaliacao'}
              mensagem={null}
              aoEnviar={(nota, comentario) =>
                executar(
                  'avaliacao',
                  () => avaliarPrestador(prefixoTipado, idServico, { nota, comentario }),
                  'Avaliação enviada. Ela é imutável.',
                )
              }
            />
          )}

          {(s.status === 'CHEGOU' || s.status === 'FINALIZADO') && (
            <SecaoConclusao
              servico={s}
              lado="entusiasta"
              rotuloOutro={rotuloPrestador}
              confirmando={acaoEmAndamento === 'conclusao'}
              mensagem={null}
              aoConfirmar={() =>
                executar('conclusao', () => confirmarConclusaoColecionador(prefixoTipado, idServico), 'Conclusão confirmada por você.')
              }
            />
          )}

          {/* Cancelamento — R6/N3: SOLICITADO/PROPOSTA imediato; PENDENTE após 2 h; CHEGOU/FINALIZADO bloqueado */}
          {s.status === 'PENDENTE' && minutosRestantes > 0 && (
            <Alerta tipo="aviso">
              O {rotuloPrestador} ainda está a caminho. O cancelamento fica disponível em {minutosRestantes} minuto(s).
            </Alerta>
          )}
          {(s.status === 'SOLICITADO' || s.status === 'PROPOSTA' || s.status === 'PENDENTE') && (
            <div className="detalhe-secao">
              <h2>Cancelar solicitação</h2>
              <Botao
                variante="perigo"
                disabled={!podeCancelar}
                carregando={acaoEmAndamento === 'cancelar'}
                onClick={() => setConfirmandoCancelar(true)}
              >
                Cancelar solicitação
              </Botao>
              {s.status === 'PENDENTE' && minutosRestantes > 0 && (
                <p className="abreviado">
                  Em PENDENTE o cancelamento só fica disponível 2 horas após o aceite (a regra final é do backend).
                </p>
              )}
            </div>
          )}
        </>
      )}

      <Modal
        aberto={confirmandoCancelar}
        titulo="Cancelar solicitação"
        confirmarRotulo="Sim, cancelar"
        varianteConfirmar="perigo"
        confirmarCarregando={acaoEmAndamento === 'cancelar'}
        aoConfirmar={() => void aoCancelar()}
        aoFechar={() => setConfirmandoCancelar(false)}
      >
        <p>O cancelamento remove fisicamente a solicitação. Deseja continuar?</p>
      </Modal>
    </div>
  )
}

/** F26 — ranking de prestadores compatíveis (paginado; ordem notaMatch desc etc. — vem pronta). */
function RankingPrestadores({ idServico }: { idServico: string }) {
  const { envelope, carregando, erro, irParaPagina, atualizar } = usePaginacao<PrestadorCompativelDTO>(
    (pagina, tamanho) => listarPrestadoresCompativeis(idServico, pagina, tamanho),
    5,
    [idServico],
  )

  return (
    <div>
      {carregando && <LoadingPagina rotulo="Carregando ranking..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}
      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Nenhum prestador compatível" descricao="Nenhum prestador elegível com vaga disponível na região." />
      )}

      {envelope.conteudo.map((prestador) => (
        <article key={prestador.idPrestador} className="card">
          <div className="card-cabecalho">
            <h2 className="card-titulo">{prestador.nomePrestador}</h2>
            <div className="botoes-inline">
              <span className="badge badge-roxo">{LABEL_TIPO_PRESTADOR[prestador.tipoPrestador]}</span>
              <BadgeNotaMatch notaMatch={prestador.notaMatch} />
            </div>
          </div>
          <div className="card-corpo">
            <div className="linha-dados">
              <span className="rotulo">Nota ajustada</span>
              <span>{formatarNota(prestador.notaAjustada)} ★</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Avaliações</span>
              <span>{prestador.quantidadeAvaliacoes}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Distância</span>
              <span>{formatarDistancia(prestador.distanciaKm)}</span>
            </div>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}
