import { Link } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { listarCatalogo } from '../../api/eventos'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { CatalogoEventoResponseDTO } from '../../types/api'
import { formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'

/** F6 — catálogo público de eventos ATIVO (feed), sem autenticação obrigatória.
 *  Ordem do feed = dataHoraInicio asc (vem pronta — não reordenar). */
export function Catalogo() {
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<CatalogoEventoResponseDTO>(listarCatalogo)

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Eventos</h1>
      </div>

      {carregando && <LoadingPagina rotulo="Carregando eventos..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)} <button type="button" className="botao botao-texto" onClick={atualizar}>Tentar novamente</button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Nenhum evento ativo" descricao="Não há eventos abertos para inscrição agora." />
      )}

      {envelope.conteudo.map((evento) => (
        <CardEvento key={evento.idEvento} evento={evento} />
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}

function CardEvento({ evento }: { evento: CatalogoEventoResponseDTO }) {
  return (
    <article className="card">
      <div className="card-cabecalho">
        <div>
          <h2 className="card-titulo">{evento.nome}</h2>
          <p className="card-subtitulo">
            Organizado por <strong>{evento.nomeOrganizador}</strong> · {formatarDataHora(evento.dataHoraInicio)} até{' '}
            {formatarDataHora(evento.dataHoraFim)}
          </p>
        </div>
        <div className="botoes-inline">
          {evento.inscrito && <span className="badge badge-verde">Inscrito</span>}
          <span className={`badge ${evento.vagasDisponiveis > 0 ? 'badge-azul' : 'badge-vermelho'}`}>
            {evento.vagasDisponiveis} vaga(s)
          </span>
        </div>
      </div>

      {evento.fotos.length > 0 && (
        <img
          src={evento.fotos[0]}
          alt={`Foto do evento ${evento.nome}`}
          style={{ width: '100%', maxHeight: 220, objectFit: 'cover', borderRadius: 8 }}
        />
      )}

      <div className="card-corpo">
        <p>{evento.descricao}</p>
        <div className="linha-dados">
          <span className="rotulo">Preço</span>
          <strong>{formatarMoeda(evento.preco)}</strong>
        </div>
        <div className="linha-dados">
          <span className="rotulo">Endereço</span>
          <span>
            {evento.rua}, {evento.numero} - {evento.bairro}, {evento.cidade} - {evento.uf} ({evento.cep})
          </span>
        </div>
      </div>

      <div className="card-acoes">
        <Link to={`/eventos/${evento.idEvento}`} className="botao botao-primario" style={{ textDecoration: 'none' }}>
          Ver detalhes
        </Link>
        {evento.latitude !== null && evento.longitude !== null && (
          <a
            className="botao botao-secundario"
            style={{ textDecoration: 'none' }}
            href={`https://www.google.com/maps?q=${evento.latitude},${evento.longitude}`}
            target="_blank"
            rel="noreferrer"
          >
            Ver no mapa
          </a>
        )}
      </div>
    </article>
  )
}
