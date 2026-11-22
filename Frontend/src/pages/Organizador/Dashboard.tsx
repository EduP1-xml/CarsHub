import { useEffect, useState } from 'react'
import { Alerta } from '../../components/Alerta'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { BadgeStatusEvento } from '../../components/BadgeStatus'
import { buscarMetricasDashboard, listarHistoricoDashboard } from '../../api/eventos'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { HistoricoEventoResponseDTO } from '../../types/api'
import { extrairErro, mensagemDoErro, type ErroNormalizado } from '../../utils/erros'

/**
 * F34 — dashboard do organizador:
 * totalParticipantesMobilizados = inscrições CONFIRMADAS;
 * histórico = CONCLUÍDO + CANCELADO mesclados (dataHoraFim desc — vem pronto),
 * totalParticipantes conta SÓ CONFIRMADAS.
 */
export function Dashboard() {
  const [metricas, setMetricas] = useState<{ totalEventosCriados: number; totalParticipantesMobilizados: number } | null>(null)
  const [erroMetricas, setErroMetricas] = useState<ErroNormalizado | null>(null)

  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<HistoricoEventoResponseDTO>(listarHistoricoDashboard)

  useEffect(() => {
    buscarMetricasDashboard()
      .then((resposta) => setMetricas(resposta.data))
      .catch((e) => setErroMetricas(extrairErro(e)))
  }, [envelope])

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Dashboard</h1>
      </div>

      {erroMetricas && <Alerta tipo="erro">{mensagemDoErro(erroMetricas)}</Alerta>}

      <div className="form-grid" style={{ marginBottom: 20 }}>
        <div className="card">
          <h2 className="card-titulo">Eventos criados</h2>
          <p style={{ fontSize: '2rem', fontWeight: 800, margin: '8px 0' }}>
            {metricas?.totalEventosCriados ?? '—'}
          </p>
        </div>
        <div className="card">
          <h2 className="card-titulo">Participantes mobilizados</h2>
          <p style={{ fontSize: '2rem', fontWeight: 800, margin: '8px 0' }}>
            {metricas?.totalParticipantesMobilizados ?? '—'}
          </p>
          <p className="abreviado">Inscrições CONFIRMADAS de todos os seus eventos.</p>
        </div>
      </div>

      <h2>Histórico (concluídos + cancelados)</h2>
      {carregando && <LoadingPagina rotulo="Carregando histórico..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Histórico vazio" descricao="Nenhum evento concluído ou cancelado ainda." />
      )}

      {envelope.conteudo.map((item) => (
        <article key={item.idEvento} className="card">
          <div className="card-cabecalho">
            <h2 className="card-titulo">{item.nome}</h2>
            <BadgeStatusEvento status={item.status} />
          </div>
          <div className="card-corpo">
            <div className="linha-dados">
              <span className="rotulo">Vagas (limite)</span>
              <span>{item.limiteVagas}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Participantes (CONFIRMADAS)</span>
              <span>{item.totalParticipantes}</span>
            </div>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}
