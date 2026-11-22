import { Link } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { BadgeStatusServico, BadgeTipoServico } from '../../components/BadgeStatus'
import { listarFilaDeTrabalho, type PrefixoChamado } from '../../api/servicos'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { ServicoRespostaDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F37 — fila de trabalho do prestador: ordem PENDENTE → CHEGOU → PROPOSTA → FINALIZADO
 * (mais recente no grupo) vem pronta do backend — não reordenar.
 */
export function FilaTrabalho({ prefixo }: { prefixo: PrefixoChamado }) {
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<ServicoRespostaDTO>((pagina, tamanho) => listarFilaDeTrabalho(prefixo, pagina, tamanho), 5, [prefixo])

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Minha fila de trabalho</h1>
      </div>

      {carregando && <LoadingPagina rotulo="Carregando fila..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Fila vazia" descricao="Você ainda não participa de nenhum chamado." />
      )}

      {envelope.conteudo.map((servico) => (
        <article key={servico.idServico} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">{servico.carroDescricao}</h2>
              <p className="card-subtitulo">Colecionador: {servico.nomeEntusiasta}</p>
            </div>
            <div className="botoes-inline">
              <BadgeTipoServico tipo={servico.tipoServico} />
              <BadgeStatusServico status={servico.status} />
            </div>
          </div>
          <div className="card-corpo">
            {servico.servicosSolicitados.length > 0 && (
              <div className="chips" style={{ marginBottom: 8 }}>
                {servico.servicosSolicitados.map((serv) => (
                  <span key={serv} className="chip">
                    {LABEL_SERVICOS_OFERTADOS[serv]}
                  </span>
                ))}
              </div>
            )}
            {servico.valorProposto !== null && (
              <div className="linha-dados">
                <span className="rotulo">Valor</span>
                <strong>{formatarMoeda(servico.valorProposto)}</strong>
              </div>
            )}
            <div className="linha-dados">
              <span className="rotulo">Solicitado em</span>
              <span>{formatarDataHora(servico.dataHoraSolicitacao)}</span>
            </div>
          </div>
          <div className="card-acoes">
            <Link
              to={`/prestador/servicos/${servico.idServico}`}
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
