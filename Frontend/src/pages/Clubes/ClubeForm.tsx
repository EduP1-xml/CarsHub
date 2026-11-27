import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { criarClube, detalharClube, editarClube } from '../../api/clubes'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { FiltroCarroForm } from '../../components/FiltroCarroForm'
import { CampoArea, CampoTexto } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { UploadImagens } from '../../components/UploadImagens'
import {
  estadoParaFiltro,
  estadoVazio,
  faixasInvertidas,
  filtroParaEstado,
  possuiCriterio,
  type EstadoFiltro,
} from '../../components/filtroCarro'
import { useToast } from '../../context/ToastContext'
import type { ClubeRespostaDTO } from '../../types/api'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * Criar (POST multipart) / editar (PUT multipart, só o admin) um clube.
 * Fotos: 0 a 5. O gosto precisa de pelo menos uma característica.
 * Na edição, carros vinculados que deixarem de atender ao novo gosto são desvinculados.
 */
export function ClubeForm() {
  const { idClube } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const ehEdicao = !!idClube

  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [filtro, setFiltro] = useState<EstadoFiltro>(estadoVazio())
  const [arquivos, setArquivos] = useState<File[]>([])
  const [original, setOriginal] = useState<ClubeRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(ehEdicao)
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [errosCampos, setErrosCampos] = useState<Record<string, string>>({})

  useEffect(() => {
    if (!idClube) return
    let cancelado = false
    detalharClube(idClube)
      .then((resposta) => {
        if (cancelado) return
        const clube = resposta.data
        setOriginal(clube)
        setNome(clube.nome)
        setDescricao(clube.descricao ?? '')
        setFiltro(filtroParaEstado(clube.filtro))
      })
      .catch((erro) => {
        if (!cancelado) setMensagem(mensagemDoErro(erro))
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [idClube])

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setErrosCampos({})

    // Espelho de UX (a regra final é o backend: 422).
    const dtoFiltro = estadoParaFiltro(filtro)
    if (!possuiCriterio(dtoFiltro)) {
      setMensagem('Marque pelo menos uma característica de carro para definir o gosto do clube.')
      return
    }
    const invertidas = faixasInvertidas(dtoFiltro)
    if (invertidas.length > 0) {
      setMensagem(`Faixa invertida (mínimo maior que o máximo): ${invertidas.join(', ')}.`)
      return
    }

    const dados = {
      nome: nome.trim(),
      descricao: descricao.trim() || null,
      filtro: dtoFiltro,
      fotosClube: original?.fotosClube ?? null,
    }

    setEnviando(true)
    try {
      if (ehEdicao && idClube) {
        const resposta = await editarClube(idClube, dados, arquivos)
        toast('sucesso', 'Clube atualizado.')
        if (resposta.data.carrosDesvinculados > 0) {
          toast('info', `${resposta.data.carrosDesvinculados} carro(s) deixaram de atender ao gosto e foram desvinculados.`)
        }
        navigate(`/clubes/${idClube}`)
      } else {
        const resposta = await criarClube(dados, arquivos)
        toast('sucesso', 'Clube criado! Você é o administrador.')
        navigate(`/clubes/${resposta.data.idClube}`)
      }
    } catch (erro) {
      const normalizado = extrairErro(erro)
      if (normalizado.campos) setErrosCampos(normalizado.campos)
      else setMensagem(mensagemDoErro(erro))
    } finally {
      setEnviando(false)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando clube..." />

  if (ehEdicao && original && !original.souAdministrador) {
    return <Alerta tipo="erro">Apenas o administrador do clube pode editá-lo.</Alerta>
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{ehEdicao ? 'Editar clube' : 'Criar clube'}</h1>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <form onSubmit={aoEnviar}>
        <div className="form-secao">
          <h2>Dados do clube</h2>
          <div className="form-grid">
            <div className="campo-largo">
              <CampoTexto
                id="nome"
                rotulo="Nome (máx 50)"
                obrigatorio
                maxLength={50}
                value={nome}
                erro={errosCampos.nome}
                onChange={(e) => setNome(e.target.value)}
              />
            </div>
            <div className="campo-largo">
              <CampoArea
                id="descricao"
                rotulo="Descrição (opcional, máx 500)"
                maxLength={500}
                rows={3}
                value={descricao}
                erro={errosCampos.descricao}
                onChange={(e) => setDescricao(e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="form-secao">
          <h2>Fotos do clube (até 5 — jpg/jpeg/png)</h2>
          <UploadImagens
            maximo={5}
            arquivos={arquivos}
            aoMudar={setArquivos}
            urlsAtuais={ehEdicao && arquivos.length === 0 ? original?.fotosClube ?? [] : []}
          />
          {ehEdicao && arquivos.length > 0 && <p className="abreviado">As fotos selecionadas substituirão todas as atuais.</p>}
        </div>

        <div className="form-secao">
          <h2>Gosto do clube</h2>
          <div className="help-bloco">
            Marque as características que importam para o clube. O que ficar desmarcado aceita qualquer valor.
            Os eventos da aba Eventos e os carros que os membros podem vincular seguem este gosto.
            {ehEdicao && ' Carros já vinculados que deixarem de atender ao novo gosto serão desvinculados.'}
          </div>
          <FiltroCarroForm valor={filtro} aoMudar={setFiltro} idBase="clube" />
        </div>

        <div className="form-acoes">
          <Botao type="submit" carregando={enviando}>
            {ehEdicao ? 'Salvar alterações' : 'Criar clube'}
          </Botao>
          <Botao variante="texto" onClick={() => navigate(ehEdicao ? `/clubes/${idClube}` : '/clubes')}>
            Cancelar
          </Botao>
        </div>
      </form>
    </div>
  )
}
