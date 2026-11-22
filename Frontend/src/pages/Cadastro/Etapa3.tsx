import { useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { reenviarCodigo, verificarRegistro } from '../../api/auth'
import { useCountdown } from '../../hooks/useCountdown'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasDigitosCodigo } from '../../utils/validacoes'

interface Etapa3Props {
  email: string
  aoConcluir: () => void
}

/** F1 etapa 3 + F2 (reenvio com cooldown de 60 s — 422 "Aguarde X segundo(s)..."). */
export function Etapa3({ email, aoConcluir }: Etapa3Props) {
  const [codigo, setCodigo] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [sucessoReenvio, setSucessoReenvio] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [reenviando, setReenviando] = useState(false)
  const { restante, ativo, iniciar } = useCountdown()

  async function aoConfirmar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setEnviando(true)
    try {
      await verificarRegistro({ email, codigo })
      aoConcluir()
    } catch (erro) {
      const normalizado = extrairErro(erro)
      setMensagem(mensagemDoErro(erro))
      if (normalizado.status === 422 && normalizado.mensagem?.includes('expirado')) {
        setMensagem(normalizado.mensagem)
      }
    } finally {
      setEnviando(false)
    }
  }

  async function aoReenviar() {
    setMensagem(null)
    setSucessoReenvio(null)
    setReenviando(true)
    try {
      const resposta = await reenviarCodigo({ email })
      setSucessoReenvio(resposta.data)
      // Cooldown de 60 s espelhado (a regra final é o 422 do backend).
      iniciar(60)
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setReenviando(false)
    }
  }

  return (
    <div className="form-secao">
      <div className="help-bloco">
        Enviamos um código de 6 dígitos para <strong>{email}</strong>. Ele é válido por 10 minutos.
      </div>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {sucessoReenvio && <Alerta tipo="sucesso">{sucessoReenvio}</Alerta>}

      <form className="form-grid" onSubmit={aoConfirmar}>
        <CampoTexto
          id="codigo"
          rotulo="Código de verificação"
          obrigatorio
          inputMode="numeric"
          maxLength={6}
          placeholder="123456"
          value={codigo}
          onChange={(e) => setCodigo(apenasDigitosCodigo(e.target.value))}
        />
        <div className="campo-largo form-acoes">
          <Botao type="submit" carregando={enviando} disabled={codigo.length !== 6}>
            Confirmar
          </Botao>
          <Botao variante="secundario" carregando={reenviando} disabled={ativo} onClick={aoReenviar}>
            {ativo ? `Reenviar código (${restante}s)` : 'Reenviar código'}
          </Botao>
        </div>
      </form>
    </div>
  )
}
