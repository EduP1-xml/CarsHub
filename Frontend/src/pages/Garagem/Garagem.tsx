import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CarrosselFotos } from '../../components/CarrosselFotos'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import { buscarCarro, listarCarros, removerCarro } from '../../api/carros'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { CarroRespostaDTO } from '../../types/api'
import { mensagemDoErro } from '../../utils/erros'

/**
 * F13 — garagem (listagem RESUMIDA: idCarro, marca, modelo, placa, nomeProprietario).
 * A virada do título: 1º carro → vira COLECIONADOR; excluir o ÚLTIMO → volta a ENTUSIASTA.
 * A UI reage após POST/DELETE (o idUsuario/token NÃO mudam na virada).
 */
export function Garagem() {
  const navigate = useNavigate()
  const { atualizarSessaoLocal } = useAuth()
  const { toast } = useToast()
  const { envelope, carregando, erro, irParaPagina, atualizar } =
    usePaginacao<CarroRespostaDTO>(listarCarros)

  const [excluindo, setExcluindo] = useState<CarroRespostaDTO | null>(null)
  const [excluindoAgora, setExcluindoAgora] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  // A listagem da garagem vem SEM as fotos (projeção resumida do backend):
  // buscamos o detalhe de cada carro da página para alimentar o carrossel.
  const [fotosPorCarro, setFotosPorCarro] = useState<Record<string, string[]>>({})

  useEffect(() => {
    let cancelado = false
    const ids = envelope.conteudo
      .map((carro) => carro.idCarro)
      .filter((idCarro) => !(idCarro in fotosPorCarro))
    if (ids.length === 0) return

    Promise.allSettled(
      ids.map((idCarro) =>
        buscarCarro(idCarro).then((resposta) => [idCarro, resposta.data.fotosCarro] as const),
      ),
    ).then((resultados) => {
      if (cancelado) return
      setFotosPorCarro((atual) => {
        const proximo = { ...atual }
        resultados.forEach((resultado, indice) => {
          // Falha ao buscar o detalhe → mostra o card como "sem fotos".
          proximo[ids[indice]] = resultado.status === 'fulfilled' ? resultado.value[1] : []
        })
        return proximo
      })
    })

    return () => {
      cancelado = true
    }
  }, [envelope.conteudo, fotosPorCarro])

  async function aoExcluir() {
    if (!excluindo) return
    setExcluindoAgora(true)
    setMensagem(null)
    try {
      await removerCarro(excluindo.idCarro)
      toast('sucesso', 'Carro removido da garagem.')
      setExcluindo(null)
      // Refetch e virada do título (último carro → volta a entusiasta).
      const resposta = await listarCarros(0, envelope.tamanho)
      if (resposta.data.totalElementos === 0) {
        atualizarSessaoLocal({ tipoUsuario: 'ENTUSIASTA' })
        toast('info', 'Sua garagem ficou vazia: você voltou a ser Entusiasta.')
      }
      atualizar()
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setExcluindoAgora(false)
    }
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Minha garagem</h1>
        <Botao onClick={() => navigate('/garagem/novo')}>+ Adicionar carro</Botao>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {carregando && <LoadingPagina rotulo="Carregando garagem..." />}
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
          titulo="Garagem vazia"
          descricao="Cadastre o seu primeiro carro para se tornar colecionador e usar os serviços automotivos."
          acao={
            <Botao onClick={() => navigate('/garagem/novo')}>Cadastre seu primeiro carro</Botao>
          }
        />
      )}

      {envelope.conteudo.map((carro) => (
        <article key={carro.idCarro} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">
                {carro.marca} {carro.modelo}
              </h2>
              <p className="card-subtitulo">Placa {carro.placa}</p>
            </div>
          </div>
          <div className="card-corpo card-corpo-carro">
            <div className="card-info-carro">
              <div className="linha-dados">
                <span className="rotulo">Proprietário</span>
                <span>{carro.nomeProprietario}</span>
              </div>
            </div>
            <div className="card-fotos-carro">
              {fotosPorCarro[carro.idCarro] !== undefined ? (
                <CarrosselFotos
                  fotos={fotosPorCarro[carro.idCarro]}
                  alt={`${carro.marca} ${carro.modelo}`}
                />
              ) : (
                <div className="carrossel carrossel-carregando">
                  <span>Carregando fotos...</span>
                </div>
              )}
            </div>
          </div>
          <div className="card-acoes">
            <Link to={`/garagem/${carro.idCarro}`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
              Ver detalhe
            </Link>
            <Link to={`/garagem/${carro.idCarro}/editar`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
              Editar
            </Link>
            <Link to={`/garagem/${carro.idCarro}/historico`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
              Histórico
            </Link>
            <Botao variante="perigo" onClick={() => setExcluindo(carro)}>
              Excluir
            </Botao>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <Modal
        aberto={excluindo !== null}
        titulo="Excluir carro"
        confirmarRotulo="Excluir"
        varianteConfirmar="perigo"
        confirmarCarregando={excluindoAgora}
        aoConfirmar={aoExcluir}
        aoFechar={() => setExcluindo(null)}
      >
        <p>
          Excluir <strong>{excluindo?.marca} {excluindo?.modelo}</strong> ({excluindo?.placa})? Se for o seu último
          carro, você voltará a ser Entusiasta e perderá o acesso às telas de garagem e serviços.
        </p>
      </Modal>
    </div>
  )
}
