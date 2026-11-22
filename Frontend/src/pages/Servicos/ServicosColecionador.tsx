import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { BadgeStatusServico, BadgeTipoServico } from '../../components/BadgeStatus'
import { listarPainelConsolidado } from '../../api/servicos'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { ServicoRespostaDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F16/F25 — painel consolidado: mecânico + guincho com status ≠ FINALIZADO,
 * dataHoraSolicitacao desc (janela — ordem vem pronta, não reordenar).
 */
export function ServicosColecionador() {
  const navigate = useNavigate()
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<ServicoRespostaDTO>(listarPainelConsolidado)

  const prefixoDe = (tipo: string) => (tipo === 'MECANICO' ? 'mecanicos' : 'guinchos')

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Meus serviços</h1>
        <div className="botoes-inline">
          <Botao onClick={() => navigate('/servicos/novo-mecanico')}>+ Serviço mecânico</Botao>
          <Botao onClick={() => navigate('/servicos/novo-guincho')}>+ Solicitar guincho</Botao>
        </div>
      </div>

      {carregando && <LoadingPagina rotulo="Carregando serviços..." />}
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
          titulo="Nenhum serviço em aberto"
          descricao="Solicite um serviço mecânico ou um guincho para o seu veículo."
        />
      )}

      {envelope.conteudo.map((servico) => (
        <article key={servico.idServico} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">{servico.carroDescricao}</h2>
              <p className="card-subtitulo">Solicitado em {formatarDataHora(servico.dataHoraSolicitacao)}</p>
            </div>
            <div className="botoes-inline">
              <BadgeTipoServico tipo={servico.tipoServico} />
              <BadgeStatusServico status={servico.status} />
            </div>
          </div>
          <div className="card-corpo">
            {servico.servicosSolicitados.length > 0 && (
              <div className="chips" style={{ marginBottom: 8 }}>
                {servico.servicosSolicitados.map((servicoOf) => (
                  <span key={servicoOf} className="chip">
                    {LABEL_SERVICOS_OFERTADOS[servicoOf]}
                  </span>
                ))}
              </div>
            )}
            {servico.valorProposto !== null && (
              <div className="linha-dados">
                <span className="rotulo">Proposta</span>
                <strong>{formatarMoeda(servico.valorProposto)}</strong>
              </div>
            )}
            {servico.nomePrestador && (
              <div className="linha-dados">
                <span className="rotulo">Prestador</span>
                <span>{servico.nomePrestador}</span>
              </div>
            )}
          </div>
          <div className="card-acoes">
            <Link
              to={`/servicos/${prefixoDe(servico.tipoServico)}/${servico.idServico}`}
              className="botao botao-primario"
              style={{ textDecoration: 'none' }}
            >
              Ver detalhe
            </Link>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}
