import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listarClubes, listarMeusClubes } from '../../api/clubes'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { EstadoVazio } from '../../components/EstadoVazio'
import { FiltroCarroResumo } from '../../components/FiltroCarroResumo'
import { LoadingPagina } from '../../components/Loading'
import { Paginacao } from '../../components/Paginacao'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { ClubeRespostaDTO } from '../../types/api'
import { mensagemDoErro } from '../../utils/erros'
import { ModalEntrarClube } from './ModaisClube'

type Aba = 'todos' | 'meus'

/** Clubes: "Todos" (busca + Entrar) e "Meus clubes". Qualquer entusiasta pode criar um clube. */
export function Clubes() {
  const navigate = useNavigate()
  const [aba, setAba] = useState<Aba>('todos')
  const [textoBusca, setTextoBusca] = useState('')
  const [busca, setBusca] = useState('')
  const [entrando, setEntrando] = useState<ClubeRespostaDTO | null>(null)

  const { envelope, carregando, erro, irParaPagina, atualizar } = usePaginacao<ClubeRespostaDTO>(
    (pagina, tamanho) => (aba === 'todos' ? listarClubes(pagina, tamanho, busca) : listarMeusClubes(pagina, tamanho)),
    5,
    [aba, busca],
  )

  function aoBuscar(evento: FormEvent) {
    evento.preventDefault()
    setBusca(textoBusca)
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Clubes</h1>
        <Botao onClick={() => navigate('/clubes/novo')}>+ Criar clube</Botao>
      </div>

      <div className="tabs">
        <button type="button" className={`tab${aba === 'todos' ? ' tab-ativa' : ''}`} onClick={() => setAba('todos')}>
          Todos
        </button>
        <button type="button" className={`tab${aba === 'meus' ? ' tab-ativa' : ''}`} onClick={() => setAba('meus')}>
          Meus clubes
        </button>
      </div>

      {aba === 'todos' && (
        <form className="busca-clubes" onSubmit={aoBuscar}>
          <input
            className="campo-input"
            placeholder="Buscar clube pelo nome"
            aria-label="Buscar clube pelo nome"
            value={textoBusca}
            onChange={(e) => setTextoBusca(e.target.value)}
          />
          <Botao type="submit" variante="secundario">
            Buscar
          </Botao>
        </form>
      )}

      {carregando && <LoadingPagina rotulo="Carregando clubes..." />}
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
          titulo={aba === 'todos' ? 'Nenhum clube encontrado' : 'Você ainda não participa de clubes'}
          descricao={aba === 'todos' ? 'Que tal criar o primeiro clube com o seu gosto?' : 'Entre em um clube na aba "Todos".'}
          acao={<Botao onClick={() => navigate('/clubes/novo')}>Criar clube</Botao>}
        />
      )}

      {envelope.conteudo.map((clube) => (
        <CardClube key={clube.idClube} clube={clube} aoEntrar={() => setEntrando(clube)} />
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <ModalEntrarClube
        clube={entrando}
        aoFechar={() => setEntrando(null)}
        aoEntrar={(clube) => {
          setEntrando(null)
          navigate(`/clubes/${clube.idClube}`)
        }}
      />
    </div>
  )
}

function CardClube({ clube, aoEntrar }: { clube: ClubeRespostaDTO; aoEntrar: () => void }) {
  return (
    <article className="card">
      <div className="card-cabecalho">
        <div>
          <h2 className="card-titulo">{clube.nome}</h2>
          <p className="card-subtitulo">
            Administrado por <strong>{clube.nomeAdministrador}</strong> · {clube.quantidadeMembros} membro(s) ·{' '}
            {clube.quantidadeCarrosVinculados} carro(s)
          </p>
        </div>
        <div className="botoes-inline">
          {clube.souAdministrador && <span className="badge badge-roxo">Administrador</span>}
          {clube.souMembro && !clube.souAdministrador && <span className="badge badge-verde">Membro</span>}
        </div>
      </div>

      {clube.fotosClube.length > 0 && (
        <img
          src={clube.fotosClube[0]}
          alt={`Foto do clube ${clube.nome}`}
          style={{ width: '100%', maxHeight: 220, objectFit: 'cover', borderRadius: 8 }}
        />
      )}

      <div className="card-corpo">
        {clube.descricao && <p>{clube.descricao}</p>}
        <FiltroCarroResumo filtro={clube.filtro} />
      </div>

      <div className="card-acoes">
        <Link to={`/clubes/${clube.idClube}`} className="botao botao-primario" style={{ textDecoration: 'none' }}>
          Ver clube
        </Link>
        {!clube.souMembro && (
          <Botao variante="secundario" onClick={aoEntrar}>
            Entrar
          </Botao>
        )}
      </div>
    </article>
  )
}
