import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { redefinirSenha } from '../../api/auth'
import { useToast } from '../../context/ToastContext'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasDigitosCodigo, REGRAS_SENHA_TEXTO, validarSenhaForte } from '../../utils/validacoes'

/** F5 — redefinir senha: { email, codigo, novaSenha }.
 *  E-mail inexistente E código errado respondem a MESMA mensagem 422 "Código de verificação inválido.". */
export function RedefinirSenha() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const [email, setEmail] = useState(params.get('email') ?? '')
  const [codigo, setCodigo] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)

    const politica = validarSenhaForte(novaSenha)
    if (!politica.valida) {
      setMensagem(`Senha fraca: ${politica.problemas.join(', ')}.`)
      return
    }

    setEnviando(true)
    try {
      const resposta = await redefinirSenha({ email: email.trim(), codigo, novaSenha })
      toast('sucesso', resposta.data)
      navigate('/login')
    } catch (erro) {
      const normalizado = extrairErro(erro)
      setMensagem(mensagemDoErro(erro))
      void normalizado
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina-publica">
      <h1>Redefinir senha</h1>
      <p className="subtitulo">Informe o código recebido e a nova senha</p>

      <div className="form-secao">
        {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
        <form className="form-grid" onSubmit={aoEnviar}>
          <CampoTexto
            id="email"
            rotulo="E-mail"
            type="email"
            obrigatorio
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <CampoTexto
            id="codigo"
            rotulo="Código de verificação"
            obrigatorio
            inputMode="numeric"
            maxLength={6}
            value={codigo}
            onChange={(e) => setCodigo(apenasDigitosCodigo(e.target.value))}
          />
          <div className="campo-largo">
            <CampoTexto
              id="novaSenha"
              rotulo="Nova senha"
              type="password"
              obrigatorio
              autoComplete="new-password"
              value={novaSenha}
              ajuda={REGRAS_SENHA_TEXTO}
              onChange={(e) => setNovaSenha(e.target.value)}
            />
          </div>
          <div className="campo-largo">
            <Botao type="submit" carregando={enviando} style={{ width: '100%' }}>
              Redefinir senha
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
