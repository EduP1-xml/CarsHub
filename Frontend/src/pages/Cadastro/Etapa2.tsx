import { useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { cadastrarIndividual } from '../../api/auth'
import { useForm } from '../../hooks/useForm'
import type { ServicosOfertados, TipoPrestadorServico, TipoUsuario } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS, OPCOES_SERVICOS_OFERTADOS } from '../../utils/enums'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasAlfanumericos, apenasDigitos } from '../../utils/validacoes'

interface FormEtapa2 {
  cpf: string
  cnpj: string
  placa: string
  servicosOfertados: ServicosOfertados[]
}

interface Etapa2Props {
  email: string
  tipoUsuario: TipoUsuario
  tipoPrestador: TipoPrestadorServico | null
  aoConcluir: () => void
}

/** F1 etapa 2 — campos por tipo: ENTUSIASTA→cpf; ORGANIZADOR→cnpj; MECANICO→cnpj+serviços; GUINCHO→cnpj+placa. */
export function Etapa2({ email, tipoUsuario, tipoPrestador, aoConcluir }: Etapa2Props) {
  const { valores, setValor, errosCampos, setErrosCampos } = useForm<FormEtapa2>({
    cpf: '',
    cnpj: '',
    placa: '',
    servicosOfertados: [],
  })
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  function alternarServico(servico: ServicosOfertados) {
    const marcados = valores.servicosOfertados.includes(servico)
      ? valores.servicosOfertados.filter((s) => s !== servico)
      : [...valores.servicosOfertados, servico]
    setValor('servicosOfertados', marcados)
  }

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setErrosCampos({})

    setEnviando(true)
    try {
      await cadastrarIndividual({
        email,
        cpf: tipoUsuario === 'ENTUSIASTA' ? valores.cpf : null,
        cnpj: tipoUsuario !== 'ENTUSIASTA' ? valores.cnpj : null,
        placa: tipoPrestador === 'GUINCHO' ? valores.placa : null,
        servicosOfertados: tipoPrestador === 'MECANICO' ? valores.servicosOfertados : null,
      })
      aoConcluir()
    } catch (erro) {
      const normalizado = extrairErro(erro)
      if (normalizado.campos) {
        setErrosCampos(normalizado.campos)
      } else {
        setMensagem(mensagemDoErro(erro))
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="form-secao">
      <div className="help-bloco">
        Complete o cadastro para <strong>{email}</strong> como{' '}
        <strong>{tipoUsuario === 'PRESTADOR_SERVICO' ? `prestador de serviços (${tipoPrestador})` : tipoUsuario.replace('_', ' ').toLowerCase()}</strong>.
      </div>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <form className="form-grid" onSubmit={aoEnviar}>
        {tipoUsuario === 'ENTUSIASTA' && (
          <CampoTexto
            id="cpf"
            rotulo="CPF (11 dígitos)"
            obrigatorio
            inputMode="numeric"
            value={valores.cpf}
            erro={errosCampos.cpf}
            onChange={(e) => setValor('cpf', apenasDigitos(e.target.value, 11))}
          />
        )}

        {tipoUsuario !== 'ENTUSIASTA' && (
          <CampoTexto
            id="cnpj"
            rotulo="CNPJ (14 dígitos)"
            obrigatorio
            inputMode="numeric"
            value={valores.cnpj}
            erro={errosCampos.cnpj}
            onChange={(e) => setValor('cnpj', apenasDigitos(e.target.value, 14))}
          />
        )}

        {tipoPrestador === 'GUINCHO' && (
          <CampoTexto
            id="placa"
            rotulo="Placa do guincho (7 alfanuméricos)"
            obrigatorio
            maxLength={7}
            value={valores.placa}
            erro={errosCampos.placa}
            onChange={(e) => setValor('placa', apenasAlfanumericos(e.target.value, 7))}
          />
        )}

        {tipoPrestador === 'MECANICO' && (
          <div className="campo-largo">
            <span className="campo-rotulo">
              Serviços ofertados (selecione ao menos 1) <span className="campo-obrigatorio">*</span>
            </span>
            <div className="chips" style={{ marginTop: 6 }}>
              {OPCOES_SERVICOS_OFERTADOS.map((servico) => {
                const marcado = valores.servicosOfertados.includes(servico)
                return (
                  <label key={servico} className={`chip chip-clicavel${marcado ? ' chip-marcado' : ''}`}>
                    <input
                      type="checkbox"
                      checked={marcado}
                      onChange={() => alternarServico(servico)}
                      style={{ marginRight: 4 }}
                    />
                    {LABEL_SERVICOS_OFERTADOS[servico]}
                  </label>
                )
              })}
            </div>
            {errosCampos.servicosOfertados && <span className="campo-erro">{errosCampos.servicosOfertados}</span>}
          </div>
        )}

        <div className="campo-largo form-acoes">
          <Botao type="submit" carregando={enviando}>
            Continuar
          </Botao>
        </div>
      </form>
    </div>
  )
}
