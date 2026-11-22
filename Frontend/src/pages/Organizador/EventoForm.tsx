import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoArea, CampoTexto } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { UploadImagens } from '../../components/UploadImagens'
import {
  atualizarEventoParcial,
  buscarMeuEvento,
  criarEvento,
  substituirEvento,
} from '../../api/eventos'
import { useForm } from '../../hooks/useForm'
import { useToast } from '../../context/ToastContext'
import type { EventoPatchDTO, EventoRegistroDTO, EventoRespostaDTO } from '../../types/api'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasDigitos, paraInputDatetimeLocal, paraIsoLocalDateTime } from '../../utils/validacoes'

interface FormEvento {
  nome: string
  descricao: string
  preco: string
  limiteParticipantes: string
  rua: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  uf: string
  cep: string
  dataHoraInicio: string
  dataHoraFim: string
}

const FORM_VAZIO: FormEvento = {
  nome: '',
  descricao: '',
  preco: '',
  limiteParticipantes: '',
  rua: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  cep: '',
  dataHoraInicio: '',
  dataHoraFim: '',
}

function eventoParaForm(evento: EventoRespostaDTO): FormEvento {
  return {
    nome: evento.nome,
    descricao: evento.descricao,
    preco: String(evento.preco),
    limiteParticipantes: String(evento.limiteParticipantes),
    rua: evento.rua,
    numero: evento.numero,
    complemento: evento.complemento ?? '',
    bairro: evento.bairro,
    cidade: evento.cidade,
    uf: evento.uf,
    cep: evento.cep,
    dataHoraInicio: paraInputDatetimeLocal(evento.dataHoraInicio),
    dataHoraFim: paraInputDatetimeLocal(evento.dataHoraFim),
  }
}

function formParaRegistro(v: FormEvento): EventoRegistroDTO {
  return {
    nome: v.nome.trim(),
    descricao: v.descricao.trim(),
    preco: Number(v.preco),
    limiteParticipantes: Number(v.limiteParticipantes),
    rua: v.rua.trim(),
    numero: v.numero.trim(),
    complemento: v.complemento.trim() || null,
    bairro: v.bairro.trim(),
    cidade: v.cidade.trim(),
    uf: v.uf.trim().toUpperCase(),
    cep: v.cep.trim(),
    dataHoraInicio: paraIsoLocalDateTime(v.dataHoraInicio),
    dataHoraFim: paraIsoLocalDateTime(v.dataHoraFim),
    urlFotoEvento: null,
  }
}

/**
 * F29 — criar evento (POST multipart dados+arquivos, máx 3; geocodificação bloqueante
 * antes do upload; datas @Future; fim > início → 422). Sucesso 201 já inclui as fotos.
 * F31 — editar: PUT (multipart; regeocodifica SÓ se o endereço mudou) / PATCH (Bean
 * Validation completo, campos vazios = não alterar). Mudar dataHoraInicio de evento já
 * começado → 422.
 */
export function EventoForm() {
  const { idEvento } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { valores, setValor, reset, errosCampos, setErrosCampos } = useForm<FormEvento>(FORM_VAZIO)

  const [original, setOriginal] = useState<EventoRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState<'POST' | 'PUT' | 'PATCH' | null>(null)
  const [arquivos, setArquivos] = useState<File[]>([])

  const ehEdicao = !!idEvento

  useEffect(() => {
    if (!idEvento) {
      setCarregando(false)
      return
    }
    let cancelado = false
    buscarMeuEvento(idEvento)
      .then((resposta) => {
        if (cancelado) return
        setOriginal(resposta.data)
        reset(eventoParaForm(resposta.data))
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idEvento])

  function tratarErro(erro: unknown) {
    const normalizado = extrairErro(erro)
    if (normalizado.campos) {
      setErrosCampos(normalizado.campos)
      setMensagem(null)
    } else {
      setMensagem(mensagemDoErro(erro))
    }
  }

  async function aoCriar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setErrosCampos({})

    // Espelho de UX (a regra final é o backend: 422 fim ≤ início; @Future na criação).
    const inicio = paraIsoLocalDateTime(valores.dataHoraInicio)
    const fim = paraIsoLocalDateTime(valores.dataHoraFim)
    if (inicio && fim && fim <= inicio) {
      setMensagem('A data/hora de término do evento não pode ser igual ou anterior à data de início.')
      return
    }

    setEnviando('POST')
    try {
      const resposta = await criarEvento(formParaRegistro(valores), arquivos)
      toast('sucesso', 'Evento criado com sucesso!')
      navigate(`/meus-eventos/${resposta.data.idEvento}`)
    } catch (erro) {
      tratarErro(erro)
    } finally {
      setEnviando(null)
    }
  }

  /** PATCH: envia apenas os campos alterados (vazios/nulos = não alterar). */
  async function aoSalvarParcial() {
    if (!idEvento || !original) return
    setMensagem(null)
    setErrosCampos({})

    const patch: EventoPatchDTO = {}
    const atual = formParaRegistro(valores)
    const comparar = (campo: keyof EventoRegistroDTO, anterior: unknown, novo: unknown) => {
      if (JSON.stringify(anterior) !== JSON.stringify(novo)) {
        ;(patch as Record<string, unknown>)[campo] = novo
      }
    }
    comparar('nome', original.nome, atual.nome)
    comparar('descricao', original.descricao, atual.descricao)
    comparar('preco', original.preco, atual.preco)
    comparar('limiteParticipantes', original.limiteParticipantes, atual.limiteParticipantes)
    comparar('rua', original.rua, atual.rua)
    comparar('numero', original.numero, atual.numero)
    comparar('complemento', original.complemento, atual.complemento)
    comparar('bairro', original.bairro, atual.bairro)
    comparar('cidade', original.cidade, atual.cidade)
    comparar('uf', original.uf, atual.uf)
    comparar('cep', original.cep, atual.cep)
    comparar('dataHoraInicio', original.dataHoraInicio, atual.dataHoraInicio)
    comparar('dataHoraFim', original.dataHoraFim, atual.dataHoraFim)

    setEnviando('PATCH')
    try {
      await atualizarEventoParcial(idEvento, patch, arquivos)
      toast('sucesso', 'Evento atualizado (PATCH).')
      navigate(`/meus-eventos/${idEvento}`)
    } catch (erro) {
      tratarErro(erro)
    } finally {
      setEnviando(null)
    }
  }

  /** PUT: substituição completa; fotos novas (arquivos, máx 3) substituem todas. */
  async function aoSalvarCompleto() {
    if (!idEvento || !original) return
    setMensagem(null)
    setErrosCampos({})

    setEnviando('PUT')
    try {
      const registro = formParaRegistro(valores)
      await substituirEvento(idEvento, { ...registro, urlFotoEvento: original.urlFotoEvento }, arquivos)
      toast('sucesso', 'Evento substituído (PUT).')
      navigate(`/meus-eventos/${idEvento}`)
    } catch (erro) {
      tratarErro(erro)
    } finally {
      setEnviando(null)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando formulário..." />

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{ehEdicao ? 'Editar evento' : 'Criar evento'}</h1>
        {ehEdicao && (
          <Botao variante="secundario" onClick={() => navigate(`/meus-eventos/${idEvento}`)}>
            ← Voltar ao detalhe
          </Botao>
        )}
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <form className="form-secao" onSubmit={ehEdicao ? (e) => e.preventDefault() : aoCriar}>
        <h2>Dados do evento</h2>
        <div className="form-grid">
          <CampoTexto
            id="nome"
            rotulo="Nome (máx 50)"
            obrigatorio
            maxLength={50}
            value={valores.nome}
            erro={errosCampos.nome}
            onChange={(e) => setValor('nome', e.target.value)}
          />
          <CampoTexto
            id="preco"
            rotulo="Preço (R$, ≥ 0,00)"
            obrigatorio
            type="number"
            step="0.01"
            min="0"
            value={valores.preco}
            erro={errosCampos.preco}
            onChange={(e) => setValor('preco', e.target.value)}
          />
          <CampoTexto
            id="limiteParticipantes"
            rotulo="Limite de participantes (≥ 1)"
            obrigatorio
            type="number"
            min="1"
            value={valores.limiteParticipantes}
            erro={errosCampos.limiteParticipantes}
            onChange={(e) => setValor('limiteParticipantes', e.target.value)}
          />
          <div className="campo-largo">
            <CampoArea
              id="descricao"
              rotulo="Descrição (máx 1000)"
              obrigatorio
              maxLength={1000}
              rows={4}
              value={valores.descricao}
              erro={errosCampos.descricao}
              onChange={(e) => setValor('descricao', e.target.value)}
            />
          </div>
        </div>

        <h2>Endereço (usado na geocodificação do pino do mapa)</h2>
        <div className="help-bloco">
          A geocodificação é <strong>bloqueante</strong>: ao salvar, o backend consulta o mapa com o endereço completo
          (rua, numero - bairro, cidade - UF, CEP). Pode levar alguns segundos.
        </div>
        <div className="form-grid">
          <div className="campo-largo">
            <CampoTexto
              id="rua"
              rotulo="Rua (máx 100)"
              obrigatorio
              maxLength={100}
              value={valores.rua}
              erro={errosCampos.rua}
              onChange={(e) => setValor('rua', e.target.value)}
            />
          </div>
          <CampoTexto
            id="numero"
            rotulo="Número (máx 5)"
            obrigatorio
            maxLength={5}
            value={valores.numero}
            erro={errosCampos.numero}
            onChange={(e) => setValor('numero', e.target.value)}
          />
          <CampoTexto
            id="complemento"
            rotulo="Complemento (opcional, máx 50)"
            maxLength={50}
            value={valores.complemento}
            erro={errosCampos.complemento}
            onChange={(e) => setValor('complemento', e.target.value)}
          />
          <CampoTexto
            id="bairro"
            rotulo="Bairro (máx 50)"
            obrigatorio
            maxLength={50}
            value={valores.bairro}
            erro={errosCampos.bairro}
            onChange={(e) => setValor('bairro', e.target.value)}
          />
          <CampoTexto
            id="cidade"
            rotulo="Cidade (máx 50)"
            obrigatorio
            maxLength={50}
            value={valores.cidade}
            erro={errosCampos.cidade}
            onChange={(e) => setValor('cidade', e.target.value)}
          />
          <CampoTexto
            id="uf"
            rotulo="UF (2 letras)"
            obrigatorio
            maxLength={2}
            value={valores.uf}
            erro={errosCampos.uf}
            onChange={(e) => setValor('uf', e.target.value.toUpperCase())}
          />
          <CampoTexto
            id="cep"
            rotulo="CEP (8 dígitos)"
            obrigatorio
            inputMode="numeric"
            value={valores.cep}
            erro={errosCampos.cep}
            onChange={(e) => setValor('cep', apenasDigitos(e.target.value, 8))}
          />
        </div>

        <h2>Datas</h2>
        <div className="form-grid">
          <CampoTexto
            id="dataHoraInicio"
            rotulo="Data/hora de início"
            obrigatorio
            type="datetime-local"
            value={valores.dataHoraInicio}
            erro={errosCampos.dataHoraInicio}
            onChange={(e) => setValor('dataHoraInicio', e.target.value)}
            ajuda={ehEdicao ? 'Mudar o início de um evento já começado → 422 do backend.' : 'Deve estar no futuro (@Future).'}
          />
          <CampoTexto
            id="dataHoraFim"
            rotulo="Data/hora de fim"
            obrigatorio
            type="datetime-local"
            value={valores.dataHoraFim}
            erro={errosCampos.dataHoraFim}
            onChange={(e) => setValor('dataHoraFim', e.target.value)}
            ajuda="Deve ser posterior ao início."
          />
        </div>

        <h2>Fotos (máx. 3 — jpg/jpeg/png)</h2>
        <UploadImagens
          maximo={3}
          arquivos={arquivos}
          aoMudar={setArquivos}
          urlsAtuais={ehEdicao && arquivos.length === 0 ? original?.urlFotoEvento ?? [] : []}
        />
        {ehEdicao && arquivos.length > 0 && (
          <p className="abreviado">As fotos selecionadas substituirão todas as atuais.</p>
        )}

        <div className="form-acoes">
          {!ehEdicao && (
            <Botao type="submit" carregando={enviando === 'POST'}>
              Criar evento
            </Botao>
          )}
          {ehEdicao && (
            <>
              <Botao type="submit" carregando={enviando === 'PATCH'} onClick={() => void aoSalvarParcial()}>
                Salvar alterações (PATCH)
              </Botao>
              <Botao
                variante="secundario"
                carregando={enviando === 'PUT'}
                onClick={() => void aoSalvarCompleto()}
                title="Substituição completa: envia todos os campos; o endereço é regeocodificado só se mudou"
              >
                Substituir tudo (PUT)
              </Botao>
            </>
          )}
          <Botao variante="texto" onClick={() => navigate(ehEdicao ? `/meus-eventos/${idEvento}` : '/meus-eventos')}>
            Cancelar
          </Botao>
        </div>
      </form>
    </div>
  )
}
