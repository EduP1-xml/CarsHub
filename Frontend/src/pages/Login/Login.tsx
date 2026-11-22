import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect, CampoTexto } from '../../components/FormField'
import { rotaInicialParaTipo, useAuth } from '../../context/AuthContext'
import { useCountdown } from '../../hooks/useCountdown'
import { useForm } from '../../hooks/useForm'
import type { TipoPrestadorServico, TipoUsuario } from '../../types/api'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

interface FormLogin {
  login: string
  senha: string
  tipoUsuarioSelecionado: TipoUsuario | ''
  tipoPrestador: TipoPrestadorServico | ''
}

// O perfil nasce ENTUSIASTA e vira COLECIONADOR com o 1º carro — quem já tem
// garagem entra escolhendo "Entusiasta" (o backend devolve o tipo real).
const OPCOES_PERFIL: { valor: TipoUsuario; rotulo: string }[] = [
  { valor: 'ENTUSIASTA', rotulo: 'Entusiasta' },
  { valor: 'ORGANIZADOR_EVENTO', rotulo: 'Organizador de eventos' },
  { valor: 'PRESTADOR_SERVICO', rotulo: 'Prestador de serviços' },
]

const OPCOES_SUBTIPO: { valor: TipoPrestadorServico; rotulo: string }[] = [
  { valor: 'MECANICO', rotulo: 'Mecânico' },
  { valor: 'GUINCHO', rotulo: 'Guincho' },
]

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { valores, setValor, errosCampos, setErrosCampos, limparErros } = useForm<FormLogin>({
    login: '',
    senha: '',
    tipoUsuarioSelecionado: '',
    tipoPrestador: '',
  })
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const { restante, ativo, iniciar } = useCountdown()

  const ehPrestador = valores.tipoUsuarioSelecionado === 'PRESTADOR_SERVICO'

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setErrosCampos({})
    limparErros()

    // Espelho da regra do backend (400 "Selecione o tipo de prestador de serviço...").
    if (ehPrestador && !valores.tipoPrestador) {
      setMensagem('Selecione o tipo de prestador de serviço (Mecânico ou Guincho).')
      return
    }

    setEnviando(true)
    try {
      const resposta = await login({
        login: valores.login.trim(),
        senha: valores.senha,
        tipoUsuarioSelecionado: valores.tipoUsuarioSelecionado as TipoUsuario,
        tipoPrestador: ehPrestador ? (valores.tipoPrestador as TipoPrestadorServico) : null,
      })
      // Sucesso: sessão gravada no AuthContext (carshub:auth) — navegar para a home do perfil.
      navigate(rotaInicialParaTipo(resposta.tipoUsuario, resposta.tipoPrestadorServico), { replace: true })
    } catch (erro) {
      const normalizado = extrairErro(erro)
      if (normalizado.campos) {
        setErrosCampos(normalizado.campos)
        setMensagem(null)
      } else {
        setMensagem(mensagemDoErro(erro))
      }
      // 429: respeitar o Retry-After — desabilitar o botão e contar.
      if (normalizado.status === 429 && normalizado.retryAfter) {
        iniciar(normalizado.retryAfter)
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina-publica">
      <h1>🚗 CarsHub</h1>
      <p className="subtitulo">Entre com a sua conta</p>

      <div className="form-secao">
        {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
        {ativo && (
          <Alerta tipo="aviso">
            Muitas tentativas. Aguarde {restante} segundo(s) antes de tentar novamente.
          </Alerta>
        )}

        <form className="form-grid" onSubmit={aoEnviar}>
          <div className="campo-largo">
            <CampoTexto
              id="login"
              rotulo="E-mail"
              type="email"
              autoComplete="username"
              obrigatorio
              value={valores.login}
              erro={errosCampos.login}
              onChange={(e) => setValor('login', e.target.value)}
              placeholder="voce@email.com"
            />
          </div>
          <div className="campo-largo">
            <CampoTexto
              id="senha"
              rotulo="Senha"
              type="password"
              autoComplete="current-password"
              obrigatorio
              value={valores.senha}
              erro={errosCampos.senha}
              onChange={(e) => setValor('senha', e.target.value)}
            />
          </div>
          <CampoSelect
            id="tipoUsuarioSelecionado"
            rotulo="Tipo de perfil"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES_PERFIL}
            value={valores.tipoUsuarioSelecionado}
            erro={errosCampos.tipoUsuarioSelecionado}
            onChange={(e) => {
              setValor('tipoUsuarioSelecionado', e.target.value as TipoUsuario | '')
              setValor('tipoPrestador', '')
            }}
          />
          {ehPrestador && (
            <CampoSelect
              id="tipoPrestador"
              rotulo="Tipo de prestador"
              obrigatorio
              placeholder="Selecione..."
              opcoes={OPCOES_SUBTIPO}
              value={valores.tipoPrestador}
              erro={errosCampos.tipoPrestador}
              onChange={(e) => setValor('tipoPrestador', e.target.value as TipoPrestadorServico | '')}
            />
          )}
          <div className="campo-largo">
            <Botao type="submit" carregando={enviando} disabled={ativo} style={{ width: '100%' }}>
              Entrar
            </Botao>
          </div>
        </form>
      </div>

      <p className="links-rodape">
        <Link to="/esqueci-senha">Esqueci minha senha</Link> · <Link to="/confirmar-codigo">Confirmar código</Link> ·{' '}
        <Link to="/cadastro">Criar conta</Link>
      </p>
    </div>
  )
}
