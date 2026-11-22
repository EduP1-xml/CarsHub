import { useState } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { UploadImagens } from '../../components/UploadImagens'
import { enviarFotoPerfil, type Ator } from '../../api/perfis'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/** F9/F28/F35 — foto de perfil: multipart "arquivo" (1 imagem). 201 → texto com a URL.
 *  Formato inválido → 415; nome com ".." → 400; > 100MB → 413. */
export function AbaFoto({ ator }: { ator: Ator }) {
  const { usuario, atualizarSessaoLocal } = useAuth()
  const { toast } = useToast()
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function aoEnviar() {
    if (!arquivo) return
    setMensagem(null)
    setEnviando(true)
    try {
      const resposta = await enviarFotoPerfil(ator, arquivo)
      // A resposta é a URL em texto (regra F9) — a URL SEMPRE vem do backend.
      const url = resposta.data.trim()
      atualizarSessaoLocal({ urlFotoPerfil: url })
      toast('sucesso', 'Foto de perfil atualizada com sucesso.')
      setArquivo(null)
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
      void extrairErro(erro)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="form-secao">
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <div className="help-bloco">Envie uma imagem JPG, JPEG ou PNG de até 100MB.</div>

      {usuario?.urlFotoPerfil && (
        <img
          src={usuario.urlFotoPerfil}
          alt="Foto de perfil atual"
          style={{ width: 110, height: 110, objectFit: 'cover', borderRadius: '50%', marginBottom: 14 }}
        />
      )}

      <UploadImagens
        maximo={1}
        unico
        arquivos={arquivo ? [arquivo] : []}
        aoMudar={(arquivos) => setArquivo(arquivos[0] ?? null)}
      />

      <div className="form-acoes">
        <Botao onClick={aoEnviar} carregando={enviando} disabled={!arquivo}>
          Enviar foto
        </Botao>
      </div>
    </div>
  )
}
