import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  detalharClube,
  excluirClube,
  listarCarrosDoClube,
  listarEventosDoClube,
  listarMembros,
  removerMembro,
  sairDoClube,
  transferirAdministracao,
} from '../../api/clubes'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CarrosselFotos } from '../../components/CarrosselFotos'
import { EstadoVazio } from '../../components/EstadoVazio'
import { FiltroCarroResumo } from '../../components/FiltroCarroResumo'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import { ehColecionador, useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { usePaginacao } from '../../hooks/usePaginacao'
import type {
  CarroRespostaDTO,
  CatalogoEventoResponseDTO,
  ClubeRespostaDTO,
  MembroClubeRespostaDTO,
} from '../../types/api'
import { LABEL_CARROCERIA } from '../../utils/enums'
import { formatarData } from '../../utils/formatadores'
import { mensagemDoErro } from '../../utils/erros'
import { CardEvento } from '../Catalogo/Catalogo'
import { ModalEntrarClube, ModalGerenciarCarros } from './ModaisClube'

type Aba = 'sobre' | 'carros' | 'eventos'

type Confirmacao =
  | { tipo: 'sair' }
  | { tipo: 'excluir' }
  | { tipo: 'remover'; membro: MembroClubeRespostaDTO }
  | { tipo: 'transferir'; membro: MembroClubeRespostaDTO }

/**
 * Detalhe do clube: cabeçalho (fotos, admin, contadores, ações) + abas Sobre (gosto e
 * membros), Carros (vinculados) e Eventos (compatíveis com o gosto). Membros, carros e
 * eventos só aparecem para quem é membro (o backend devolve 403 para os demais).
 */
export function ClubeDetalhe() {
  const { idClube = '' } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()

  const [clube, setClube] = useState<ClubeRespostaDTO | null>(null)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [aba, setAba] = useState<Aba>('sobre')
  const [entrando, setEntrando] = useState(false)
  const [confirmacao, setConfirmacao] = useState<Confirmacao | null>(null)
  const [executando, setExecutando] = useState(false)
  // Muda a cada ação que altera membros/carros: as abas recarregam.
  const [versao, setVersao] = useState(0)

  const carregar = useCallback(() => {
    detalharClube(idClube)
      .then((resposta) => setClube(resposta.data))
      .catch((erro) => setMensagem(mensagemDoErro(erro)))
  }, [idClube])

  useEffect(() => {
    carregar()
  }, [carregar])

  function recarregarTudo() {
    carregar()
    setVersao((v) => v + 1)
  }

  async function confirmar() {
    if (!confirmacao || !clube) return
    setExecutando(true)
    setMensagem(null)
    try {
      if (confirmacao.tipo === 'sair') {
        await sairDoClube(clube.idClube)
        toast('info', `Você saiu do clube ${clube.nome}.`)
        setAba('sobre')
        // Só o cabeçalho: sem bumpar "versao", senão a lista de membros (ainda montada
        // com souMembro antigo) refaz a busca e leva 403 antes do clube recarregar.
        setConfirmacao(null)
        carregar()
        return
      } else if (confirmacao.tipo === 'excluir') {
        await excluirClube(clube.idClube)
        toast('sucesso', 'Clube excluído.')
        navigate('/clubes')
        return
      } else if (confirmacao.tipo === 'remover') {
        await removerMembro(clube.idClube, confirmacao.membro.idEntusiasta)
        toast('sucesso', `${confirmacao.membro.nome} foi removido(a) do clube.`)
      } else {
        await transferirAdministracao(clube.idClube, confirmacao.membro.idEntusiasta)
        toast('sucesso', `${confirmacao.membro.nome} agora é o administrador do clube.`)
      }
      setConfirmacao(null)
      recarregarTudo()
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
      setConfirmacao(null)
    } finally {
      setExecutando(false)
    }
  }

  if (!clube) {
    return mensagem ? <Alerta tipo="erro">{mensagem}</Alerta> : <LoadingPagina rotulo="Carregando clube..." />
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{clube.nome}</h1>
        <div className="botoes-inline">
          {clube.souAdministrador && <span className="badge badge-roxo">Administrador</span>}
          {clube.souMembro && !clube.souAdministrador && <span className="badge badge-verde">Membro</span>}
          {!clube.souMembro && <Botao onClick={() => setEntrando(true)}>Entrar no clube</Botao>}
          {clube.souMembro && !clube.souAdministrador && (
            <Botao variante="secundario" onClick={() => setConfirmacao({ tipo: 'sair' })}>
              Sair do clube
            </Botao>
          )}
          {clube.souAdministrador && (
            <>
              <Link to={`/clubes/${clube.idClube}/editar`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
                Editar
              </Link>
              <Botao variante="perigo" onClick={() => setConfirmacao({ tipo: 'excluir' })}>
                Excluir
              </Botao>
            </>
          )}
        </div>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <div className="detalhe-carro">
        <div className="detalhe-carro-dados">
          <div className="detalhe-secao">
            {clube.descricao && <p>{clube.descricao}</p>}
            <dl className="linha-tempo">
              <dt>Administrador</dt>
              <dd>{clube.nomeAdministrador}</dd>
              <dt>Membros</dt>
              <dd>{clube.quantidadeMembros}</dd>
              <dt>Carros vinculados</dt>
              <dd>{clube.quantidadeCarrosVinculados}</dd>
              <dt>Criado em</dt>
              <dd>{formatarData(clube.dataCriacao)}</dd>
            </dl>
            {clube.souAdministrador && (
              <p className="abreviado">
                Para sair do clube, transfira a administração para outro membro (aba Sobre) ou exclua o clube.
              </p>
            )}
          </div>
        </div>
        <div className="detalhe-carro-fotos">
          <CarrosselFotos fotos={clube.fotosClube} alt={`Fotos do clube ${clube.nome}`} abrirEmNovaAba />
        </div>
      </div>

      <div className="tabs">
        <button type="button" className={`tab${aba === 'sobre' ? ' tab-ativa' : ''}`} onClick={() => setAba('sobre')}>
          Sobre
        </button>
        {clube.souMembro && (
          <>
            <button type="button" className={`tab${aba === 'carros' ? ' tab-ativa' : ''}`} onClick={() => setAba('carros')}>
              Carros
            </button>
            <button type="button" className={`tab${aba === 'eventos' ? ' tab-ativa' : ''}`} onClick={() => setAba('eventos')}>
              Eventos
            </button>
          </>
        )}
      </div>

      {aba === 'sobre' && (
        <>
          <div className="detalhe-secao">
            <h2>Gosto do clube</h2>
            <FiltroCarroResumo filtro={clube.filtro} />
          </div>
          {clube.souMembro ? (
            <AbaMembros
              clube={clube}
              versao={versao}
              aoRemover={(membro) => setConfirmacao({ tipo: 'remover', membro })}
              aoTransferir={(membro) => setConfirmacao({ tipo: 'transferir', membro })}
            />
          ) : (
            <Alerta tipo="info">Entre no clube para ver os membros, os carros e os eventos compatíveis.</Alerta>
          )}
        </>
      )}
      {aba === 'carros' && clube.souMembro && <AbaCarros clube={clube} versao={versao} aoMudar={recarregarTudo} />}
      {aba === 'eventos' && clube.souMembro && <AbaEventos idClube={clube.idClube} />}

      {entrando && (
        <ModalEntrarClube
          clube={clube}
          aoFechar={() => setEntrando(false)}
          aoEntrar={() => {
            setEntrando(false)
            recarregarTudo()
          }}
        />
      )}

      <Modal
        aberto={confirmacao !== null}
        titulo={tituloConfirmacao(confirmacao)}
        confirmarRotulo="Confirmar"
        varianteConfirmar={confirmacao?.tipo === 'transferir' ? 'primario' : 'perigo'}
        confirmarCarregando={executando}
        aoConfirmar={() => void confirmar()}
        aoFechar={() => setConfirmacao(null)}
      >
        <p>{textoConfirmacao(confirmacao, clube)}</p>
      </Modal>
    </div>
  )
}

function tituloConfirmacao(confirmacao: Confirmacao | null): string {
  switch (confirmacao?.tipo) {
    case 'sair':
      return 'Sair do clube'
    case 'excluir':
      return 'Excluir clube'
    case 'remover':
      return 'Remover membro'
    case 'transferir':
      return 'Transferir administração'
    default:
      return ''
  }
}

function textoConfirmacao(confirmacao: Confirmacao | null, clube: ClubeRespostaDTO): string {
  switch (confirmacao?.tipo) {
    case 'sair':
      return `Sair de ${clube.nome}? Os seus carros vinculados deixam o clube.`
    case 'excluir':
      return `Excluir ${clube.nome}? Membros e carros vinculados saem do clube. Esta ação não pode ser desfeita.`
    case 'remover':
      return `Remover ${confirmacao.membro.nome} do clube? Os carros vinculados dele(a) também saem.`
    case 'transferir':
      return `Tornar ${confirmacao.membro.nome} o administrador? Você continua como membro e perde os poderes de administrador.`
    default:
      return ''
  }
}

// ---------------------------------------------------------------------------
// Abas
// ---------------------------------------------------------------------------

function AbaMembros({
  clube,
  versao,
  aoRemover,
  aoTransferir,
}: {
  clube: ClubeRespostaDTO
  versao: number
  aoRemover: (membro: MembroClubeRespostaDTO) => void
  aoTransferir: (membro: MembroClubeRespostaDTO) => void
}) {
  const { envelope, carregando, erro, irParaPagina } = usePaginacao<MembroClubeRespostaDTO>(
    (pagina, tamanho) => listarMembros(clube.idClube, pagina, tamanho),
    10,
    [clube.idClube, versao],
  )

  return (
    <div className="detalhe-secao">
      <h2>Membros</h2>
      {carregando && <LoadingPagina rotulo="Carregando membros..." />}
      {erro && <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>}
      <ul className="lista-membros">
        {envelope.conteudo.map((membro) => (
          <li key={membro.idEntusiasta} className="membro-clube">
            {membro.urlFotoPerfil ? (
              <img className="navbar-avatar" src={membro.urlFotoPerfil} alt={`Foto de ${membro.nome}`} />
            ) : (
              <span className="navbar-avatar navbar-avatar-vazio">👤</span>
            )}
            <div className="membro-clube-dados">
              <strong>{membro.nome}</strong> {membro.administrador && <span className="badge badge-roxo">Admin</span>}
              <div className="abreviado">Entrou em {formatarData(membro.dataEntrada)}</div>
              {membro.carrosVinculados.length > 0 && (
                <div className="chips" style={{ marginTop: 6 }}>
                  {membro.carrosVinculados.map((carro) => (
                    <span key={carro.idCarro} className="chip">
                      {carro.marca} {carro.modelo} ({carro.ano})
                    </span>
                  ))}
                </div>
              )}
            </div>
            {clube.souAdministrador && !membro.administrador && (
              <div className="botoes-inline">
                <Botao variante="secundario" onClick={() => aoTransferir(membro)}>
                  Tornar administrador
                </Botao>
                <Botao variante="perigo" onClick={() => aoRemover(membro)}>
                  Remover
                </Botao>
              </div>
            )}
          </li>
        ))}
      </ul>
      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}

function AbaCarros({ clube, versao, aoMudar }: { clube: ClubeRespostaDTO; versao: number; aoMudar: () => void }) {
  const { usuario } = useAuth()
  const [gerenciando, setGerenciando] = useState(false)
  const { envelope, carregando, erro, irParaPagina } = usePaginacao<CarroRespostaDTO>(
    (pagina, tamanho) => listarCarrosDoClube(clube.idClube, pagina, tamanho),
    6,
    [clube.idClube, versao],
  )

  return (
    <div>
      {ehColecionador(usuario) && (
        <div className="form-acoes" style={{ marginTop: 0, marginBottom: 12 }}>
          <Botao onClick={() => setGerenciando(true)}>Gerenciar meus carros</Botao>
        </div>
      )}
      {carregando && <LoadingPagina rotulo="Carregando carros..." />}
      {erro && <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>}
      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Nenhum carro vinculado" descricao="Os membros ainda não vincularam carros a este clube." />
      )}
      {envelope.conteudo.map((carro) => (
        <article key={carro.idCarro} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">
                {carro.marca} {carro.modelo}
              </h2>
              <p className="card-subtitulo">
                {carro.ano} · {carro.cor} · {LABEL_CARROCERIA[carro.carroceria]}
              </p>
            </div>
          </div>
          <div className="card-corpo card-corpo-carro">
            <div className="card-info-carro">
              <div className="linha-dados">
                <span className="rotulo">Dono</span>
                <span>{carro.nomeProprietario}</span>
              </div>
            </div>
            <div className="card-fotos-carro">
              <CarrosselFotos fotos={carro.fotosCarro.filter(Boolean)} alt={`${carro.marca} ${carro.modelo}`} />
            </div>
          </div>
        </article>
      ))}
      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <ModalGerenciarCarros
        idClube={clube.idClube}
        aberto={gerenciando}
        aoFechar={() => setGerenciando(false)}
        aoSalvar={() => {
          setGerenciando(false)
          aoMudar()
        }}
      />
    </div>
  )
}

function AbaEventos({ idClube }: { idClube: string }) {
  const { envelope, carregando, erro, irParaPagina } = usePaginacao<CatalogoEventoResponseDTO>(
    (pagina, tamanho) => listarEventosDoClube(idClube, pagina, tamanho),
    5,
    [idClube],
  )

  return (
    <div>
      <div className="help-bloco">Eventos ativos compatíveis com o gosto do clube (inclui os abertos a todos os carros).</div>
      {carregando && <LoadingPagina rotulo="Carregando eventos..." />}
      {erro && <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>}
      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio titulo="Nenhum evento compatível" descricao="Não há eventos ativos para o gosto deste clube agora." />
      )}
      {envelope.conteudo.map((evento) => (
        <CardEvento key={evento.idEvento} evento={evento} />
      ))}
      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />
    </div>
  )
}
