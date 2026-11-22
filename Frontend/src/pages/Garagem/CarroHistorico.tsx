import { Link, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { BadgeTipoServico } from '../../components/BadgeStatus'
import { listarHistoricoManutencoes } from '../../api/carros'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { HistoricoManutencaoResponseDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F27 — histórico de manutenções do veículo: janela (mecânico + guincho), só FINALIZADO,
 * do mais recente para o mais antigo (vem pronto — não reordenar).
 */
export function CarroHistorico() {
  const { idCarro = '' } = useParams()
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<HistoricoManutencaoResponseDTO>(
      (pagina, tamanho) => listarHistoricoManutencoes(idCarro, pagina, tamanho),
      5,
      [idCarro],
    )

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Histórico do veículo</h1>
        <Link to={`/garagem/${idCarro}`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar ao carro
        </Link>
      </div>

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
        <EstadoVazio titulo="Sem manutenções" descricao="Nenhum serviço finalizado para este veículo ainda." />
      )}

      {envelope.conteudo.map((item) => (
        <article key={item.idServico} className="card">
          <div className="card-cabecalho">
            <h2 className="card-titulo">{item.nomePrestador}</h2>
            <BadgeTipoServico tipo={item.tipoServico} />
          </div>
          <div className="card-corpo">
            {item.servicosRealizados.length > 0 && (
              <div className="linha-dados">
                <span className="rotulo">Serviços</span>
                <span className="chips">
                  {item.servicosRealizados.map((servico) => (
                    <span key={servico} className="chip">
                      {LABEL_SERVICOS_OFERTADOS[servico]}
                    </span>
                  ))}
                </span>
              </div>
            )}
            <div className="linha-dados">
              <span className="rotulo">Local</span>
              <span>{item.enderecoOcorrencia ?? '—'}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Valor</span>
              <span>{formatarMoeda(item.valorProposto)}</span>
            </div>
            <div className="linha-dados">
              <span className="rotulo">Finalizado em</span>
              <span>{formatarDataHora(item.dataHoraFinalizacao)}</span>
            </div>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}
