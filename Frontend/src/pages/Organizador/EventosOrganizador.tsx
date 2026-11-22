import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import { BadgeStatusEvento } from '../../components/BadgeStatus'
import { excluirEvento, listarMeusEventos } from '../../api/eventos'
import { usePaginacao } from '../../hooks/usePaginacao'
import { useToast } from '../../context/ToastContext'
import type { EventoRespostaDTO } from '../../types/api'
import { formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * F30 — meus eventos (paginado, dataHoraInicio desc).
 * F33 — excluir: com inscrições → 409 ("Os dados informados conflitam com um registro
 * já existente.") — orientar cancelar inscrições ou usar o status CANCELADO.
 */
export function EventosOrganizador() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<EventoRespostaDTO>(listarMeusEventos)

  const [excluindo, setExcluindo] = useState<EventoRespostaDTO | null>(null)
  const [excluindoAgora, setExcluindoAgora] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  async function aoExcluir() {
    if (!excluindo) return
    setExcluindoAgora(true)
    setMensagem(null)
    try {
      await excluirEvento(excluindo.idEvento)
      toast('sucesso', 'Evento excluído.')
      setExcluindo(null)
      atualizar()
    } catch (erroEx) {
      const normalizado = extrairErro(erroEx)
      setMensagem(mensagemDoErro(erroEx))
      if (normalizado.status === 409) {
        setMensagem(
          `${mensagemDoErro(erroEx)} Se o evento já tem inscrições, cancele-as ou use o status CANCELADO em vez de excluir.`,
        )
      }
      setExcluindo(null)
    } finally {
      setExcluindoAgora(false)
    }
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Meus eventos</h1>
        <Botao onClick={() => navigate('/meus-eventos/novo')}>+ Criar evento</Botao>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {carregando && <LoadingPagina rotulo="Carregando eventos..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Nenhum evento" descricao="Crie o seu primeiro evento para mobilizar a comunidade." />
      )}

      {envelope.conteudo.map((evento) => (
        <article key={evento.idEvento} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">{evento.nome}</h2>
              <p className="card-subtitulo">
                {formatarDataHora(evento.dataHoraInicio)} → {formatarDataHora(evento.dataHoraFim)} ·{' '}
                {formatarMoeda(evento.preco)}
              </p>
            </div>
            <BadgeStatusEvento status={evento.status} />
          </div>
          <div className="card-corpo">
            <div className="linha-dados">
              <span className="rotulo">Local</span>
              <span>
                {evento.rua}, {evento.numero} - {evento.bairro}, {evento.cidade} - {evento.uf}
              </span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Limite de participantes</span>
              <span>{evento.limiteParticipantes}</span>
            </div>
          </div>
          <div className="card-acoes">
            <Link to={`/meus-eventos/${evento.idEvento}`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
              Ver detalhe
            </Link>
            <Link to={`/meus-eventos/${evento.idEvento}/editar`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
              Editar
            </Link>
            <Botao variante="perigo" onClick={() => setExcluindo(evento)}>
              Excluir
            </Botao>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <Modal
        aberto={excluindo !== null}
        titulo="Excluir evento"
        confirmarRotulo="Excluir"
        varianteConfirmar="perigo"
        confirmarCarregando={excluindoAgora}
        aoConfirmar={aoExcluir}
        aoFechar={() => setExcluindo(null)}
      >
        <p>
          Excluir o evento <strong>{excluindo?.nome}</strong>? Se ele já tiver inscrições, o backend bloqueará com 409 —
          nesse caso, cancele as inscrições ou use o status CANCELADO.
        </p>
      </Modal>
    </div>
  )
}
