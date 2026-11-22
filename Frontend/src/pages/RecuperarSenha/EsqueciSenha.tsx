import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { esqueciSenha } from '../../api/auth'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/** F4 — esqueci minha senha. O backend responde 200 SEMPRE com a mensagem genérica
 *  (nunca revelar se o e-mail existe) — a UI nunca mostra erro aqui. */
export function EsqueciSenha() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [mensagemSucesso, setMensagemSucesso] = useState<string | null>(null)
  const [mensagemErro, setMensagemErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagemErro(null)
    setMensagemSucesso(null)
    setEnviando(true)
    try {
      const resposta = await esqueciSenha({ email: email.trim() })
      setMensagemSucesso(resposta.data)
      // Levar o e-mail pré-preenchido para a tela de redefinição (F5).
      setTimeout(() => navigate(`/redefinir-senha?email=${encodeURIComponent(email.trim())}`), 1500)
    } catch (erro) {
      // Falhas de e-mail (502/503/504) têm mensagens próprias; exibi-las sem traduzir.
      setMensagemErro(mensagemDoErro(erro))
      void extrairErro(erro)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina-publica">
      <h1>Esqueci minha senha</h1>
      <p className="subtitulo">Informe o seu e-mail para receber um código de verificação</p>

      <div className="form-secao">
        {mensagemSucesso && <Alerta tipo="sucesso">{mensagemSucesso}</Alerta>}
        {mensagemErro && <Alerta tipo="erro">{mensagemErro}</Alerta>}
        <form className="form-grid" onSubmit={aoEnviar}>
          <CampoTexto
            id="email"
            rotulo="E-mail"
            type="email"
            obrigatorio
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="voce@email.com"
          />
          <div className="campo-largo">
            <Botao type="submit" carregando={enviando} style={{ width: '100%' }}>
              Enviar código
            </Botao>
          </div>
        </form>
      </div>

      <p className="links-rodape">
        <Link to="/login">Voltar para o login</Link>
      </p>
    </div>
  )
}
