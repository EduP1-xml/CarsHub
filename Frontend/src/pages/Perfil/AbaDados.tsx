import { useEffect, useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import {
  atualizarPerfilParcial,
  buscarPerfil,
  substituirPerfil,
  type Ator,
  type PerfilPatch,
  type PerfilPut,
} from '../../api/perfis'
import { useAuth } from '../../context/AuthContext'
import { useForm } from '../../hooks/useForm'
import { useToast } from '../../context/ToastContext'
import type { PerfilUsuarioDTO, ServicosOfertados } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS, OPCOES_SERVICOS_OFERTADOS } from '../../utils/enums'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasAlfanumericos } from '../../utils/validacoes'
import { formatarNota } from '../../utils/formatadores'

interface FormPerfil {
  nome: string
  telefone: string
  placa: string
  servicosOfertados: ServicosOfertados[]
}

/**
 * F7/F28/F35 — GET /{ator}/perfil pré-carrega (PerfilUsuarioDTO); PUT exige nome+telefone
 * (mecânico: serviços não vazios; guincho: placa); PATCH parcial (vazio/nulo = não alterar).
 */
export function AbaDados({ ator }: { ator: Ator }) {
  const { usuario, atualizarSessaoLocal } = useAuth()
  const { toast } = useToast()
  const { valores, setValor, reset, errosCampos, setErrosCampos } = useForm<FormPerfil>({
    nome: '',
    telefone: '',
    placa: '',
    servicosOfertados: [],
  })
  const [perfil, setPerfil] = useState<PerfilUsuarioDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState<'PUT' | 'PATCH' | null>(null)

  const ehMecanico = ator === 'mecanicos'
  const ehGuincho = ator === 'guinchos'
  const ehEntusiasta = ator === 'entusiastas'

  useEffect(() => {
    let cancelado = false
    buscarPerfil(ator)
      .then((resposta) => {
        if (cancelado) return
        setPerfil(resposta.data)
        reset({ nome: resposta.data.nome, telefone: resposta.data.telefone, placa: '', servicosOfertados: [] })
        atualizarSessaoLocal({ urlFotoPerfil: resposta.data.urlFotoPerfil ?? null })
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
  }, [ator])

  async function aoSalvar(modo: 'PUT' | 'PATCH', evento?: FormEvent) {
    evento?.preventDefault()
    setMensagem(null)
    setErrosCampos({})

    // Espelhos de UX (a regra final é o backend).
    if (modo === 'PUT' && !valores.nome.trim()) {
      setMensagem('O nome é obrigatório para a substituição completa.')
      return
    }
    if (modo === 'PUT' && !valores.telefone.trim()) {
      setMensagem('O telefone é obrigatório para a substituição completa.')
      return
    }
    if (ehMecanico && modo === 'PUT' && valores.servicosOfertados.length === 0) {
      setMensagem('A lista de serviços ofertados não pode ser vazia')
      return
    }
    if (ehGuincho && modo === 'PUT' && !valores.placa.trim()) {
      setMensagem('A placa é obrigatória')
      return
    }

    setEnviando(modo)
    try {
      let resposta: { data: string }
      if (modo === 'PUT') {
        const corpo: PerfilPut = { nome: valores.nome.trim(), telefone: valores.telefone.trim() } as PerfilPut
        if (ehMecanico) {
          ;(corpo as { servicosOfertados: ServicosOfertados[] }).servicosOfertados = valores.servicosOfertados
        }
        if (ehGuincho) {
          ;(corpo as { placa: string }).placa = valores.placa.trim()
        }
        resposta = await substituirPerfil(ator, corpo)
      } else {
        const corpo: PerfilPatch = {}
        if (valores.nome.trim()) corpo.nome = valores.nome.trim()
        if (valores.telefone.trim()) corpo.telefone = valores.telefone.trim()
        if (ehMecanico && valores.servicosOfertados.length > 0) {
          ;(corpo as { servicosOfertados?: ServicosOfertados[] }).servicosOfertados = valores.servicosOfertados
        }
        if (ehGuincho && valores.placa.trim()) {
          ;(corpo as { placa?: string }).placa = valores.placa.trim()
        }
        resposta = await atualizarPerfilParcial(ator, corpo)
      }
      toast('sucesso', resposta.data)
    } catch (erro) {
      const normalizado = extrairErro(erro)
      if (normalizado.campos) {
        setErrosCampos(normalizado.campos)
      } else {
        setMensagem(mensagemDoErro(erro))
      }
    } finally {
      setEnviando(null)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando perfil..." />

  return (
    <div className="form-secao">
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      <div className="linha-dados">
        <span className="rotulo">E-mail (login)</span>
        <span>{perfil?.login ?? usuario?.nomeUsuario}</span>
      </div>
      {ehEntusiasta && perfil && (
        <div className="linha-dados">
          <span className="rotulo">Sua reputação</span>
          <span>
            {perfil.notaMediaEntusiasta !== null && perfil.quantidadeAvaliacoes !== null
              ? `${formatarNota(perfil.notaMediaEntusiasta)} ★ (${perfil.quantidadeAvaliacoes} avaliação(ões))`
              : 'Sem avaliações ainda'}
          </span>
        </div>
      )}
      <hr className="separador" />

      <form className="form-grid" onSubmit={(e) => aoSalvar('PATCH', e)}>
        <CampoTexto
          id="nome"
          rotulo="Nome"
          obrigatorio
          maxLength={100}
          value={valores.nome}
          erro={errosCampos.nome}
          onChange={(e) => setValor('nome', e.target.value)}
        />
        <CampoTexto
          id="telefone"
          rotulo="Telefone"
          obrigatorio
          maxLength={17}
          value={valores.telefone}
          erro={errosCampos.telefone}
          onChange={(e) => setValor('telefone', e.target.value)}
        />

        {ehGuincho && (
          <CampoTexto
            id="placa"
            rotulo="Placa do guincho (7 alfanuméricos)"
            maxLength={7}
            value={valores.placa}
            erro={errosCampos.placa}
            ajuda="Vazio no PATCH = manter a atual."
            onChange={(e) => setValor('placa', apenasAlfanumericos(e.target.value, 7))}
          />
        )}

        {ehMecanico && (
          <div className="campo-largo">
            <span className="campo-rotulo">Serviços ofertados</span>
            <div className="chips" style={{ marginTop: 6 }}>
              {OPCOES_SERVICOS_OFERTADOS.map((servico) => {
                const marcado = valores.servicosOfertados.includes(servico)
                return (
                  <label key={servico} className={`chip chip-clicavel${marcado ? ' chip-marcado' : ''}`}>
                    <input
                      type="checkbox"
                      checked={marcado}
                      onChange={() =>
                        setValor(
                          'servicosOfertados',
                          marcado
                            ? valores.servicosOfertados.filter((s) => s !== servico)
                            : [...valores.servicosOfertados, servico],
                        )
                      }
                      style={{ marginRight: 4 }}
                    />
                    {LABEL_SERVICOS_OFERTADOS[servico]}
                  </label>
                )
              })}
            </div>
            <span className="campo-ajuda">
              No PATCH, deixar em branco = manter os atuais; marcar = substituir a lista.
            </span>
            {errosCampos.servicosOfertados && <span className="campo-erro">{errosCampos.servicosOfertados}</span>}
          </div>
        )}

        <div className="campo-largo form-acoes">
          <Botao type="submit" carregando={enviando === 'PATCH'}>
            Salvar alterações (PATCH)
          </Botao>
          <Botao
            variante="secundario"
            carregando={enviando === 'PUT'}
            onClick={(e) => void aoSalvar('PUT', undefined)}
            title="Substituição completa: envia todos os campos (nome, telefone e, se aplicável, placa/serviços)"
          >
            Substituir tudo (PUT)
          </Botao>
        </div>
      </form>
    </div>
  )
}
