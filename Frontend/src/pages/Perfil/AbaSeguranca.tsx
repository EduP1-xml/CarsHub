import { useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { alterarSenha, solicitarCodigoSenha, type Ator } from '../../api/perfis'
import { useCountdown } from '../../hooks/useCountdown'
import { useToast } from '../../context/ToastContext'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { apenasDigitosCodigo, REGRAS_SENHA_TEXTO, validarSenhaForte } from '../../utils/validacoes'

/**
 * F10/F28/F35 — trocar senha logado:
 * POST /{ator}/senha/solicitar-codigo (SEM body, cooldown 60 s → 422 "Aguarde X segundo(s)...")
 * → PATCH /{ator}/senha { codigo, novaSenha }. Política de senha igual à do cadastro.
 */
export function AbaSeguranca({ ator }: { ator: Ator }) {
  const { toast } = useToast()
  const { restante, ativo, iniciar } = useCountdown()
  const [codigo, setCodigo] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [solicitando, setSolicitando] = useState(false)
  const [alterando, setAlterando] = useState(false)

  async function aoSolicitar() {
    setMensagem(null)
    setSolicitando(true)
    try {
      const resposta = await solicitarCodigoSenha(ator)
      toast('sucesso', resposta.data)
      iniciar(60) // cooldown espelhado — a regra final é o 422 do backend
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setSolicitando(false)
    }
  }

  async function aoAlterar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)

    const politica = validarSenhaForte(novaSenha)
    if (!politica.valida) {
      setMensagem(`Senha fraca: ${politica.problemas.join(', ')}.`)
      return
    }

    setAlterando(true)
    try {
      const resposta = await alterarSenha(ator, { codigo, novaSenha })
      toast('sucesso', resposta.data)
      setCodigo('')
      setNovaSenha('')
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
      void extrairErro(erro)
    } finally {
      setAlterando(false)
    }
  }

  return (
    <div className="form-secao">
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <div className="help-bloco">
        Passo 1 — envie um código para o seu e-mail. Passo 2 — informe o código e a nova senha.
      </div>

      <div className="form-acoes">
        <Botao variante="secundario" carregando={solicitando} disabled={ativo} onClick={aoSolicitar}>
          {ativo ? `Reenviar em ${restante}s` : 'Enviar código para meu e-mail'}
        </Botao>
      </div>

      <hr className="separador" />

      <form className="form-grid" onSubmit={aoAlterar}>
        <CampoTexto
          id="codigo"
          rotulo="Código de verificação (6 dígitos)"
          inputMode="numeric"
          maxLength={6}
          value={codigo}
          onChange={(e) => setCodigo(apenasDigitosCodigo(e.target.value))}
        />
        <CampoTexto
          id="novaSenha"
          rotulo="Nova senha"
          type="password"
          autoComplete="new-password"
          value={novaSenha}
          ajuda={REGRAS_SENHA_TEXTO}
          onChange={(e) => setNovaSenha(e.target.value)}
        />
        <div className="campo-largo form-acoes">
          <Botao type="submit" carregando={alterando} disabled={codigo.length !== 6 || novaSenha.length === 0}>
            Alterar senha
          </Botao>
        </div>
      </form>
    </div>
  )
}
