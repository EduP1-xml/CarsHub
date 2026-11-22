import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import { BadgeStatusInscricao } from '../../components/BadgeStatus'
import { cancelarInscricao, listarMinhasInscricoes } from '../../api/inscricoes'
import { usePaginacao } from '../../hooks/usePaginacao'
import { useToast } from '../../context/ToastContext'
import type { InscricaoResponseDTO } from '../../types/api'
import { formatarDataHora } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/** F12 — minhas inscrições (PAGINADA, data desc) + cancelamento (PATCH /cancelar — devolve a vaga). */
export function Inscricoes() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<InscricaoResponseDTO>(listarMinhasInscricoes)
  const [cancelandoId, setCancelandoId] = useState<string | null>(null)
  const [cancelando, setCancelando] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  async function aoCancelar() {
    if (!cancelandoId) return
    setCancelando(true)
    setMensagem(null)
    try {
      const resposta = await cancelarInscricao(cancelandoId)
      toast('sucesso', resposta.data)
      setCancelandoId(null)
      atualizar()
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setCancelando(false)
    }
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Minhas inscrições</h1>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {carregando && <LoadingPagina rotulo="Carregando inscrições..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio
          titulo="Nenhuma inscrição"
          descricao="Você ainda não se inscreveu em nenhum evento. Explore o catálogo!"
          acao={
            <Botao onClick={() => navigate('/eventos')}>Ver eventos</Botao>
          }
        />
      )}

      {envelope.conteudo.map((inscricao) => (
        <article key={inscricao.idInscricao} className="card">
          <div className="card-cabecalho">
            <h2 className="card-titulo">{inscricao.nomeEvento}</h2>
            <BadgeStatusInscricao status={inscricao.status} />
          </div>
          <div className="card-corpo">
            <div className="linha-dados">
              <span className="rotulo">Data da inscrição</span>
              <span>{formatarDataHora(inscricao.dataInscricao)}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Veículo vinculado</span>
              <span>{inscricao.carroVinculado}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Entusiasta</span>
              <span>{inscricao.nomeEntusiasta}</span>
            </div>
          </div>
          {inscricao.status === 'CONFIRMADA' && (
            <div className="card-acoes">
              <Botao variante="perigo" onClick={() => setCancelandoId(inscricao.idInscricao)}>
                Cancelar inscrição
              </Botao>
            </div>
          )}
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <Modal
        aberto={cancelandoId !== null}
        titulo="Cancelar inscrição"
        confirmarRotulo="Sim, cancelar"
        varianteConfirmar="perigo"
        confirmarCarregando={cancelando}
        aoConfirmar={aoCancelar}
        aoFechar={() => setCancelandoId(null)}
      >
        <p>
          Deseja realmente cancelar esta inscrição? A vaga será devolvida ao evento e a inscrição ficará com o status
          CANCELADA.
        </p>
      </Modal>
    </div>
  )
}
