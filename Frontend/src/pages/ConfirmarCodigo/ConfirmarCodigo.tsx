import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { reenviarCodigo, verificarRegistro } from '../../api/auth'
import { useCountdown } from '../../hooks/useCountdown'
import { useToast } from '../../context/ToastContext'
import { mensagemDoErro } from '../../utils/erros'
import { apenasDigitosCodigo } from '../../utils/validacoes'

/**
 * Confirmação do código de verificação FORA do cadastro (usuário que criou a
 * conta mas não validou o e-mail). Usa os endpoints públicos do backend:
 *   POST /usuarios/cadastrar/verificarRegistro (e-mail + código)
 *   POST /usuarios/cadastrar/reenviarCodigo     (e-mail; cooldown 60 s)
 * Se a conta não verificada já foi removida pela tarefa de limpeza
 * (ContaNaoVerificadaTask), o backend responde 404 e a mensagem aparece aqui.
 */
export function ConfirmarCodigo() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const [email, setEmail] = useState('')
  const [codigo, setCodigo] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [sucessoReenvio, setSucessoReenvio] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [reenviando, setReenviando] = useState(false)
  const { restante, ativo, iniciar } = useCountdown()

  async function aoConfirmar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setSucessoReenvio(null)
    setEnviando(true)
    try {
      await verificarRegistro({ email: email.trim(), codigo })
      toast('sucesso', 'E-mail verificado com sucesso. Você já pode fazer login no CarsHub.')
      navigate('/login')
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setEnviando(false)
    }
  }

  async function aoReenviar() {
    setMensagem(null)
    setSucessoReenvio(null)
    setReenviando(true)
    try {
      const resposta = await reenviarCodigo({ email: email.trim() })
      setSucessoReenvio(resposta.data)
      iniciar(60) // Cooldown espelhado — a regra final é o 422 do backend.
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setReenviando(false)
    }
  }

  return (
    <div className="pagina-publica">
      <h1>Confirmar código</h1>
      <p className="subtitulo">Valide sua conta para concluir o cadastro</p>

      <div className="form-secao">
        <div className="help-bloco">
          Não confirmou o código na criação da conta? Informe o e-mail usado no cadastro e o código de 6 dígitos
          enviado por e-mail. Se o código expirou, use "Reenviar código".
        </div>
        {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
        {sucessoReenvio && <Alerta tipo="sucesso">{sucessoReenvio}</Alerta>}

        <form className="form-grid" onSubmit={aoConfirmar}>
          <div className="campo-largo">
            <CampoTexto
              id="email"
              rotulo="E-mail"
              type="email"
              obrigatorio
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="voce@email.com"
            />
          </div>
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
            <Botao type="submit" carregando={enviando} disabled={email.trim() === '' || codigo.length !== 6}>
              Confirmar
            </Botao>
            <Botao variante="secundario" carregando={reenviando} disabled={ativo || email.trim() === ''} onClick={aoReenviar}>
              {ativo ? `Reenviar código (${restante}s)` : 'Reenviar código'}
            </Botao>
          </div>
        </form>
      </div>

      <p className="links-rodape">
        <Link to="/login">Voltar para o login</Link> · <Link to="/cadastro">Criar conta</Link>
      </p>
    </div>
  )
}
