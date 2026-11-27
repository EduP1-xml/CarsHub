import { FiltroCarroResumo } from '../../components/FiltroCarroResumo'
import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { BadgeStatusEvento } from '../../components/BadgeStatus'
import { alterarStatusEvento, buscarMeuEvento, excluirEvento } from '../../api/eventos'
import { useToast } from '../../context/ToastContext'
import type { EventoRespostaDTO, StatusEvento } from '../../types/api'
import { LABEL_STATUS_EVENTO } from '../../utils/enums'
import { formatarCep, formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

const STATUS_EVENTO: StatusEvento[] = ['ATIVO', 'ESPERA', 'INATIVO', 'CANCELADO', 'CONCLUIDO']

/**
 * F30/F32/F33 — detalhe do evento do organizador:
 * status com os 5 valores (PATCH /status?novoStatus=... — sem validação de transição);
 * excluir com inscrições → 409 (orientar cancelar inscrições ou usar CANCELADO).
 */
export function EventoDetalhe() {
  const { idEvento = '' } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()

  const [evento, setEvento] = useState<EventoRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<unknown | null>(null)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [trocandoStatus, setTrocandoStatus] = useState(false)
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)
  const [excluindo, setExcluindo] = useState(false)

  const carregar = useCallback(async () => {
    setCarregando(true)
    setErro(null)
    try {
      const resposta = await buscarMeuEvento(idEvento)
      setEvento(resposta.data)
    } catch (e) {
      setErro(e)
    } finally {
      setCarregando(false)
    }
  }, [idEvento])

  useEffect(() => {
    void carregar()
  }, [carregar])

  async function aoMudarStatus(novoStatus: StatusEvento) {
    setMensagem(null)
    setTrocandoStatus(true)
    try {
      const resposta = await alterarStatusEvento(idEvento, novoStatus)
      setEvento(resposta.data)
      toast('sucesso', `Status alterado para ${LABEL_STATUS_EVENTO[resposta.data.status]}.`)
    } catch (e) {
      setMensagem(mensagemDoErro(e))
    } finally {
      setTrocandoStatus(false)
    }
  }

  async function aoExcluir() {
    setExcluindo(true)
    setMensagem(null)
    try {
      await excluirEvento(idEvento)
      toast('sucesso', 'Evento excluído.')
      navigate('/meus-eventos')
    } catch (e) {
      const normalizado = extrairErro(e)
      if (normalizado.status === 409) {
        setMensagem(
          `${mensagemDoErro(e)} Se o evento já tem inscrições, cancele-as ou use o status CANCELADO em vez de excluir.`,
        )
      } else {
        setMensagem(mensagemDoErro(e))
      }
      setConfirmandoExclusao(false)
    } finally {
      setExcluindo(false)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando evento..." />

  if (erro || !evento) {
    return (
      <div>
        <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>
        <Link to="/meus-eventos" className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar para meus eventos
        </Link>
      </div>
    )
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{evento.nome}</h1>
        <div className="botoes-inline">
          <BadgeStatusEvento status={evento.status} />
          <Link to={`/meus-eventos/${evento.idEvento}/editar`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
            Editar
          </Link>
          <Botao variante="perigo" onClick={() => setConfirmandoExclusao(true)}>
            Excluir
          </Botao>
        </div>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      {evento.urlFotoEvento.length > 0 && (
        <div className="galeria" style={{ marginBottom: 16 }}>
          {evento.urlFotoEvento.map((url, indice) => (
            <a key={indice} href={url} target="_blank" rel="noreferrer">
              <img src={url} alt={`Foto ${indice + 1} do evento`} />
            </a>
          ))}
        </div>
      )}

      <div className="detalhe-secao">
        <h2>Dados do evento</h2>
        <p>{evento.descricao}</p>
        <dl className="linha-tempo">
          <dt>Início</dt>
          <dd>{formatarDataHora(evento.dataHoraInicio)}</dd>
          <dt>Fim</dt>
          <dd>{formatarDataHora(evento.dataHoraFim)}</dd>
          <dt>Preço</dt>
          <dd>{formatarMoeda(evento.preco)}</dd>
          <dt>Limite de participantes</dt>
          <dd>{evento.limiteParticipantes}</dd>
          <dt>Endereço</dt>
          <dd>
            {evento.rua}, {evento.numero}
            {evento.complemento ? ` (${evento.complemento})` : ''} - {evento.bairro}
            <br />
            {evento.cidade} - {evento.uf} · CEP {formatarCep(evento.cep)}
          </dd>
          {evento.latitude !== null && evento.longitude !== null && (
            <>
              <dt>Pino do mapa</dt>
              <dd>
                {evento.latitude}, {evento.longitude}
                <div className="form-acoes">
                  <a
                    className="botao botao-secundario"
                    style={{ textDecoration: 'none' }}
                    href={`https://www.google.com/maps?q=${evento.latitude},${evento.longitude}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Ver no mapa
                  </a>
                </div>
              </dd>
            </>
          )}
        </dl>
      </div>

      <div className="detalhe-secao">
        <h2>Carros aceitos</h2>
        <FiltroCarroResumo filtro={evento.filtroCarro} vazio="Evento aberto a todos os carros." />
      </div>

      <div className="detalhe-secao">
        <h2>Status do evento</h2>
        <div className="help-bloco">
          Qualquer um dos 5 valores pode ser aplicado (sem validação de transição). Uma task automática conclui
          eventos ATIVO com data de fim no passado (a cada 30 min).
        </div>
        <CampoSelect
          id="novoStatus"
          rotulo="Alterar status"
          opcoes={STATUS_EVENTO.map((status) => ({ valor: status, rotulo: LABEL_STATUS_EVENTO[status] }))}
          value={evento.status}
          onChange={(e) => void aoMudarStatus(e.target.value as StatusEvento)}
        />
        {trocandoStatus && <p className="abreviado">Aplicando status...</p>}
      </div>

      <Modal
        aberto={confirmandoExclusao}
        titulo="Excluir evento"
        confirmarRotulo="Excluir"
        varianteConfirmar="perigo"
        confirmarCarregando={excluindo}
        aoConfirmar={aoExcluir}
        aoFechar={() => setConfirmandoExclusao(false)}
      >
        <p>
          Excluir o evento <strong>{evento.nome}</strong>? Se ele já tiver inscrições, o backend bloqueará com 409 —
          nesse caso, cancele as inscrições ou use o status CANCELADO.
        </p>
      </Modal>
    </div>
  )
}
