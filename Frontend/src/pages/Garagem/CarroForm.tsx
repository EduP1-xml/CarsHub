import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect, CampoTexto } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { UploadImagens } from '../../components/UploadImagens'
import {
  atualizarCarroParcial,
  buscarCarro,
  registrarCarro,
  substituirCarro,
} from '../../api/carros'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useForm } from '../../hooks/useForm'
import type {
  CarroPatchDTO,
  CarroRegistroDTO,
  CarroRespostaDTO,
  TipoAlimentacao,
  TipoCambio,
  TipoCarroceria,
  TipoCombustivel,
  TipoDirecao,
  TipoFreio,
  TipoInjecao,
  TipoMaterialRevestimentoInterno,
  TipoMotor,
  TipoRoda,
  TipoSuspensao,
  TipoTracao,
} from '../../types/api'
import {
  LABEL_ALIMENTACAO,
  LABEL_CAMBIO,
  LABEL_CARROCERIA,
  LABEL_COMBUSTIVEL,
  LABEL_DIRECAO,
  LABEL_FREIO,
  LABEL_INJECAO,
  LABEL_MOTOR,
  LABEL_REVESTIMENTO,
  LABEL_RODA,
  LABEL_SUSPENSAO,
  LABEL_TRACAO,
} from '../../utils/enums'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasAlfanumericos } from '../../utils/validacoes'

interface FormCarro {
  marca: string
  modelo: string
  carroceria: TipoCarroceria | ''
  ano: string
  cor: string
  quantidadeCilindros: string
  motor: TipoMotor | ''
  litragemMotor: string
  injecao: TipoInjecao | ''
  alimentacao: TipoAlimentacao | ''
  combustivel: TipoCombustivel | ''
  cambio: TipoCambio | ''
  tracao: TipoTracao | ''
  suspensao: TipoSuspensao | ''
  freio: TipoFreio | ''
  direcao: TipoDirecao | ''
  roda: TipoRoda | ''
  aro: string
  numeroPortas: string
  numeroAssentos: string
  materialRevestimentoInterno: TipoMaterialRevestimentoInterno | ''
  placa: string
  vidrosEletricos: boolean
  som: boolean
  arCondicionado: boolean
  tetoSolar: boolean
}

const FORM_VAZIO: FormCarro = {
  marca: '',
  modelo: '',
  carroceria: '',
  ano: '',
  cor: '',
  quantidadeCilindros: '',
  motor: '',
  litragemMotor: '',
  injecao: '',
  alimentacao: '',
  combustivel: '',
  cambio: '',
  tracao: '',
  suspensao: '',
  freio: '',
  direcao: '',
  roda: '',
  aro: '',
  numeroPortas: '',
  numeroAssentos: '',
  materialRevestimentoInterno: '',
  placa: '',
  vidrosEletricos: false,
  som: false,
  arCondicionado: false,
  tetoSolar: false,
}

const OPCOES = {
  carroceria: Object.entries(LABEL_CARROCERIA).map(([valor, rotulo]) => ({ valor, rotulo })),
  motor: Object.entries(LABEL_MOTOR).map(([valor, rotulo]) => ({ valor, rotulo })),
  injecao: Object.entries(LABEL_INJECAO).map(([valor, rotulo]) => ({ valor, rotulo })),
  alimentacao: Object.entries(LABEL_ALIMENTACAO).map(([valor, rotulo]) => ({ valor, rotulo })),
  combustivel: Object.entries(LABEL_COMBUSTIVEL).map(([valor, rotulo]) => ({ valor, rotulo })),
  cambio: Object.entries(LABEL_CAMBIO).map(([valor, rotulo]) => ({ valor, rotulo })),
  tracao: Object.entries(LABEL_TRACAO).map(([valor, rotulo]) => ({ valor, rotulo })),
  suspensao: Object.entries(LABEL_SUSPENSAO).map(([valor, rotulo]) => ({ valor, rotulo })),
  freio: Object.entries(LABEL_FREIO).map(([valor, rotulo]) => ({ valor, rotulo })),
  direcao: Object.entries(LABEL_DIRECAO).map(([valor, rotulo]) => ({ valor, rotulo })),
  roda: Object.entries(LABEL_RODA).map(([valor, rotulo]) => ({ valor, rotulo })),
  revestimento: Object.entries(LABEL_REVESTIMENTO).map(([valor, rotulo]) => ({ valor, rotulo })),
}

function carroParaForm(carro: CarroRespostaDTO): FormCarro {
  return {
    marca: carro.marca,
    modelo: carro.modelo,
    carroceria: carro.carroceria,
    ano: String(carro.ano),
    cor: carro.cor,
    quantidadeCilindros: String(carro.quantidadeCilindros),
    motor: carro.motor,
    litragemMotor: String(carro.litragemMotor),
    injecao: carro.injecao,
    alimentacao: carro.alimentacao,
    combustivel: carro.combustivel,
    cambio: carro.cambio,
    tracao: carro.tracao,
    suspensao: carro.suspensao,
    freio: carro.freio,
    direcao: carro.direcao,
    roda: carro.roda,
    aro: String(carro.aro),
    numeroPortas: String(carro.numeroPortas),
    numeroAssentos: String(carro.numeroAssentos),
    materialRevestimentoInterno: carro.materialRevestimentoInterno,
    placa: carro.placa,
    vidrosEletricos: carro.vidrosEletricos,
    som: carro.som,
    arCondicionado: carro.arCondicionado,
    tetoSolar: carro.tetoSolar,
  }
}

function formParaRegistro(v: FormCarro, fotosAtuais: string[] | null): CarroRegistroDTO {
  return {
    marca: v.marca.trim(),
    modelo: v.modelo.trim(),
    carroceria: v.carroceria as TipoCarroceria,
    ano: Number(v.ano),
    cor: v.cor.trim(),
    quantidadeCilindros: Number(v.quantidadeCilindros),
    motor: v.motor as TipoMotor,
    litragemMotor: Number(v.litragemMotor),
    injecao: v.injecao as TipoInjecao,
    alimentacao: v.alimentacao as TipoAlimentacao,
    combustivel: v.combustivel as TipoCombustivel,
    cambio: v.cambio as TipoCambio,
    tracao: v.tracao as TipoTracao,
    suspensao: v.suspensao as TipoSuspensao,
    freio: v.freio as TipoFreio,
    direcao: v.direcao as TipoDirecao,
    roda: v.roda as TipoRoda,
    aro: Number(v.aro),
    numeroPortas: Number(v.numeroPortas),
    numeroAssentos: Number(v.numeroAssentos),
    materialRevestimentoInterno: v.materialRevestimentoInterno as TipoMaterialRevestimentoInterno,
    placa: v.placa.trim(),
    vidrosEletricos: v.vidrosEletricos,
    som: v.som,
    arCondicionado: v.arCondicionado,
    tetoSolar: v.tetoSolar,
    // PUT: sem "arquivos" as URLs atuais (urlFotoCarro) são mantidas pelo backend.
    urlFotoCarro: fotosAtuais ?? [],
  }
}

/**
 * F13 — cadastrar carro (POST multipart dados+arquivos, máx 2) e editar
 * (PUT multipart dados+arquivos — substitui tudo; PATCH multipart dados+foto1/foto2 — troca slot a slot).
 * Faixas (400): ano = 1886 · cilindros 1..16 · litragem 0,049..15,000 · aro 8..32 · portas 1..5 · assentos 1..50.
 */
export function CarroForm() {
  const { idCarro } = useParams()
  const navigate = useNavigate()
  const { atualizarSessaoLocal } = useAuth()
  const { toast } = useToast()
  const { valores, setValor, reset, errosCampos, setErrosCampos } = useForm<FormCarro>(FORM_VAZIO)

  const [original, setOriginal] = useState<CarroRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState<'POST' | 'PUT' | 'PATCH' | null>(null)

  // Fotos
  const [arquivos, setArquivos] = useState<File[]>([]) // POST/PUT: "arquivos" (substitui todas)
  const [foto1, setFoto1] = useState<File | null>(null) // PATCH: slot índice 0
  const [foto2, setFoto2] = useState<File | null>(null) // PATCH: slot índice 1

  const ehEdicao = !!idCarro

  useEffect(() => {
    if (!idCarro) {
      setCarregando(false)
      return
    }
    let cancelado = false
    buscarCarro(idCarro)
      .then((resposta) => {
        if (cancelado) return
        setOriginal(resposta.data)
        reset(carroParaForm(resposta.data))
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
  }, [idCarro])

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
    setEnviando('POST')
    try {
      const resposta = await registrarCarro(formParaRegistro(valores, null), arquivos)
      // 1º carro → virada para COLECIONADOR (id/token NÃO mudam; só o título).
      atualizarSessaoLocal({ tipoUsuario: 'COLECIONADOR' })
      toast('sucesso', 'Carro cadastrado! Você agora é um Colecionador.')
      navigate(`/garagem/${resposta.data.idCarro}`)
    } catch (erro) {
      tratarErro(erro)
    } finally {
      setEnviando(null)
    }
  }

  /** PATCH: envia apenas os campos alterados + foto1/foto2 dos slots trocados. */
  async function aoSalvarParcial() {
    setMensagem(null)
    setErrosCampos({})
    if (!original) return

    const atual = formParaRegistro(valores, original.fotosCarro)
    const patch: CarroPatchDTO = {}
    const comparar = <K extends keyof CarroRegistroDTO>(campo: K) => {
      const anterior = original[campo as unknown as keyof CarroRespostaDTO]
      const novo = atual[campo]
      if (JSON.stringify(anterior) !== JSON.stringify(novo)) {
        ;(patch as Record<string, unknown>)[campo as string] = novo
      }
    }
    ;(
      [
        'marca', 'modelo', 'carroceria', 'ano', 'cor', 'quantidadeCilindros', 'motor', 'litragemMotor',
        'injecao', 'alimentacao', 'combustivel', 'cambio', 'tracao', 'suspensao', 'freio', 'direcao',
        'roda', 'aro', 'numeroPortas', 'numeroAssentos', 'materialRevestimentoInterno', 'placa',
        'vidrosEletricos', 'som', 'arCondicionado', 'tetoSolar',
      ] as (keyof CarroRegistroDTO)[]
    ).forEach(comparar)

    setEnviando('PATCH')
    try {
      await atualizarCarroParcial(idCarro!, patch, foto1, foto2)
      toast('sucesso', 'Carro atualizado (PATCH).')
      navigate(`/garagem/${idCarro}`)
    } catch (erro) {
      tratarErro(erro)
    } finally {
      setEnviando(null)
    }
  }

  /** PUT: substituição completa — sem "arquivos", as URLs atuais são mantidas pelo backend. */
  async function aoSalvarCompleto() {
    setMensagem(null)
    setErrosCampos({})
    if (!original) return

    setEnviando('PUT')
    try {
      await substituirCarro(idCarro!, formParaRegistro(valores, original.fotosCarro), arquivos)
      toast('sucesso', 'Carro substituído (PUT).')
      navigate(`/garagem/${idCarro}`)
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
        <h1>{ehEdicao ? 'Editar carro' : 'Cadastrar carro'}</h1>
        {ehEdicao && (
          <Botao variante="secundario" onClick={() => navigate(`/garagem/${idCarro}`)}>
            ← Voltar ao detalhe
          </Botao>
        )}
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <form className="form-secao" onSubmit={ehEdicao ? (e) => e.preventDefault() : aoCriar}>
        <h2>Identificação</h2>
        <div className="form-grid">
          <CampoTexto
            id="marca"
            rotulo="Marca"
            obrigatorio
            value={valores.marca}
            erro={errosCampos.marca}
            onChange={(e) => setValor('marca', e.target.value)}
          />
          <CampoTexto
            id="modelo"
            rotulo="Modelo"
            obrigatorio
            value={valores.modelo}
            erro={errosCampos.modelo}
            onChange={(e) => setValor('modelo', e.target.value)}
          />
          <CampoTexto
            id="placa"
            rotulo="Placa (7 alfanuméricos)"
            obrigatorio
            maxLength={7}
            value={valores.placa}
            erro={errosCampos.placa}
            onChange={(e) => setValor('placa', apenasAlfanumericos(e.target.value, 7))}
          />
          <CampoTexto
            id="ano"
            rotulo="Ano (= 1886)"
            obrigatorio
            type="number"
            value={valores.ano}
            erro={errosCampos.ano}
            onChange={(e) => setValor('ano', e.target.value)}
          />
          <CampoTexto
            id="cor"
            rotulo="Cor"
            obrigatorio
            value={valores.cor}
            erro={errosCampos.cor}
            onChange={(e) => setValor('cor', e.target.value)}
          />
        </div>

        <h2>Mecânica</h2>
        <div className="form-grid">
          <CampoSelect
            id="carroceria"
            rotulo="Carroceria"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.carroceria}
            value={valores.carroceria}
            erro={errosCampos.carroceria}
            onChange={(e) => setValor('carroceria', e.target.value as TipoCarroceria | '')}
          />
          <CampoSelect
            id="motor"
            rotulo="Motor"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.motor}
            value={valores.motor}
            erro={errosCampos.motor}
            onChange={(e) => setValor('motor', e.target.value as TipoMotor | '')}
          />
          <CampoTexto
            id="quantidadeCilindros"
            rotulo="Cilindros (1..16)"
            obrigatorio
            type="number"
            value={valores.quantidadeCilindros}
            erro={errosCampos.quantidadeCilindros}
            onChange={(e) => setValor('quantidadeCilindros', e.target.value)}
          />
          <CampoTexto
            id="litragemMotor"
            rotulo="Litragem (0,049..15,000)"
            obrigatorio
            type="number"
            step="0.001"
            value={valores.litragemMotor}
            erro={errosCampos.litragemMotor}
            onChange={(e) => setValor('litragemMotor', e.target.value)}
          />
          <CampoSelect
            id="injecao"
            rotulo="Injeção"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.injecao}
            value={valores.injecao}
            erro={errosCampos.injecao}
            onChange={(e) => setValor('injecao', e.target.value as TipoInjecao | '')}
          />
          <CampoSelect
            id="alimentacao"
            rotulo="Alimentação"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.alimentacao}
            value={valores.alimentacao}
            erro={errosCampos.alimentacao}
            onChange={(e) => setValor('alimentacao', e.target.value as TipoAlimentacao | '')}
          />
          <CampoSelect
            id="combustivel"
            rotulo="Combustível"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.combustivel}
            value={valores.combustivel}
            erro={errosCampos.combustivel}
            onChange={(e) => setValor('combustivel', e.target.value as TipoCombustivel | '')}
          />
          <CampoSelect
            id="cambio"
            rotulo="Câmbio"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.cambio}
            value={valores.cambio}
            erro={errosCampos.cambio}
            onChange={(e) => setValor('cambio', e.target.value as TipoCambio | '')}
          />
          <CampoSelect
            id="tracao"
            rotulo="Tração"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.tracao}
            value={valores.tracao}
            erro={errosCampos.tracao}
            onChange={(e) => setValor('tracao', e.target.value as TipoTracao | '')}
          />
          <CampoSelect
            id="suspensao"
            rotulo="Suspensão"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.suspensao}
            value={valores.suspensao}
            erro={errosCampos.suspensao}
            onChange={(e) => setValor('suspensao', e.target.value as TipoSuspensao | '')}
          />
          <CampoSelect
            id="freio"
            rotulo="Freio"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.freio}
            value={valores.freio}
            erro={errosCampos.freio}
            onChange={(e) => setValor('freio', e.target.value as TipoFreio | '')}
          />
          <CampoSelect
            id="direcao"
            rotulo="Direção"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.direcao}
            value={valores.direcao}
            erro={errosCampos.direcao}
            onChange={(e) => setValor('direcao', e.target.value as TipoDirecao | '')}
          />
          <CampoSelect
            id="roda"
            rotulo="Roda"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.roda}
            value={valores.roda}
            erro={errosCampos.roda}
            onChange={(e) => setValor('roda', e.target.value as TipoRoda | '')}
          />
          <CampoSelect
            id="materialRevestimentoInterno"
            rotulo="Revestimento interno"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES.revestimento}
            value={valores.materialRevestimentoInterno}
            erro={errosCampos.materialRevestimentoInterno}
            onChange={(e) => setValor('materialRevestimentoInterno', e.target.value as TipoMaterialRevestimentoInterno | '')}
          />
        </div>

        <h2>Dimensões e conforto</h2>
        <div className="form-grid">
          <CampoTexto
            id="aro"
            rotulo="Aro (8..32)"
            obrigatorio
            type="number"
            value={valores.aro}
            erro={errosCampos.aro}
            onChange={(e) => setValor('aro', e.target.value)}
          />
          <CampoTexto
            id="numeroPortas"
            rotulo="Portas (1..5)"
            obrigatorio
            type="number"
            value={valores.numeroPortas}
            erro={errosCampos.numeroPortas}
            onChange={(e) => setValor('numeroPortas', e.target.value)}
          />
          <CampoTexto
            id="numeroAssentos"
            rotulo="Assentos (1..50)"
            obrigatorio
            type="number"
            value={valores.numeroAssentos}
            erro={errosCampos.numeroAssentos}
            onChange={(e) => setValor('numeroAssentos', e.target.value)}
          />
        </div>

        <h2>Itens opcionais</h2>
        <div className="form-grid">
          {(
            [
              ['vidrosEletricos', 'Vidros elétricos'],
              ['som', 'Som'],
              ['arCondicionado', 'Ar-condicionado'],
              ['tetoSolar', 'Teto solar'],
            ] as const
          ).map(([campo, rotulo]) => (
            <label key={campo} className="chip chip-clicavel" style={{ alignSelf: 'center' }}>
              <input
                type="checkbox"
                checked={valores[campo]}
                onChange={(e) => setValor(campo, e.target.checked)}
                style={{ marginRight: 6 }}
              />
              {rotulo}
            </label>
          ))}
        </div>

        <h2>Fotos (máx. 2 — jpg/jpeg/png)</h2>
        {ehEdicao && original && (
          <>
            <div className="help-bloco">
              <strong>PATCH (salvar alterações):</strong> troque cada foto individualmente nos slots abaixo (foto1 =
              posição 1, foto2 = posição 2). <strong>PUT (substituir tudo):</strong> selecione até 2 fotos novas que
              SUBSTITUEM todas as atuais; sem novas fotos, as URLs atuais são mantidas.
            </div>
            <div className="galeria" style={{ marginBottom: 12 }}>
              {original.fotosCarro.map((url, indice) => (
                <figure key={indice} className="upload-miniatura">
                  <img src={url} alt={`Foto atual ${indice + 1}`} />
                  <figcaption className="abreviado" style={{ fontSize: '0.72rem' }}>
                    foto{indice + 1}
                  </figcaption>
                </figure>
              ))}
            </div>
            <div className="form-grid">
              <label className="campo">
                <span className="campo-rotulo">Trocar foto1 (slot 1 — PATCH)</span>
                <input
                  className="campo-input"
                  type="file"
                  accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                  onChange={(e) => setFoto1(e.target.files?.[0] ?? null)}
                />
              </label>
              <label className="campo">
                <span className="campo-rotulo">Trocar foto2 (slot 2 — PATCH)</span>
                <input
                  className="campo-input"
                  type="file"
                  accept="image/jpeg,image/png,.jpg,.jpeg,.png"
                  onChange={(e) => setFoto2(e.target.files?.[0] ?? null)}
                />
              </label>
            </div>
          </>
        )}
        <UploadImagens
          maximo={2}
          arquivos={arquivos}
          aoMudar={setArquivos}
          urlsAtuais={ehEdicao && !arquivos.length ? original?.fotosCarro ?? [] : []}
        />

        <div className="form-acoes">
          {!ehEdicao && (
            <Botao type="submit" carregando={enviando === 'POST'}>
              Cadastrar carro
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
                title="Substituição completa: envia todos os campos; fotos novas substituem todas (ou mantém as URLs atuais)"
              >
                Substituir tudo (PUT)
              </Botao>
            </>
          )}
          <Botao
            variante="texto"
            onClick={() => navigate(ehEdicao ? `/garagem/${idCarro}` : '/garagem')}
          >
            Cancelar
          </Botao>
        </div>
      </form>
    </div>
  )
}
