import { useRef, useState } from 'react'
import { Alerta } from '../Alerta'
import { Botao } from '../Botao'
import { validarImagemLocal } from '../../api/client'
import type { ServicoRespostaDTO } from '../../types/api'
import { mensagemDoErro } from '../../utils/erros'

interface SecaoFotoInicialProps {
  servico: ServicoRespostaDTO
  /** true = quem visualiza é o colecionador (dono da foto inicial). */
  aoEnviar: (arquivo: File) => Promise<void>
  enviando: boolean
  mensagem: string | null
}

/**
 * F19 — foto inicial (evidência do estado do veículo): só em PENDENTE e IMUTÁVEL
 * (a 2ª tentativa → 422 "A foto inicial já foi enviada e não pode ser substituída.").
 * Depois de enviada, o botão some (imutável).
 */
export function SecaoFotoInicial({ servico, aoEnviar, enviando, mensagem }: SecaoFotoInicialProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [erroLocal, setErroLocal] = useState<string | null>(null)

  async function aoSelecionar(lista: FileList | null) {
    const arquivo = lista?.[0]
    if (!arquivo) return
    const invalido = validarImagemLocal(arquivo)
    if (invalido) {
      setErroLocal(invalido)
      return
    }
    setErroLocal(null)
    try {
      await aoEnviar(arquivo)
    } catch (erro) {
      setErroLocal(mensagemDoErro(erro))
    } finally {
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className="detalhe-secao">
      <h2>Foto inicial (estado do veículo)</h2>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {erroLocal && <Alerta tipo="erro">{erroLocal}</Alerta>}

      {servico.urlFotoInicial ? (
        <div>
          <a href={servico.urlFotoInicial} target="_blank" rel="noreferrer">
            <img className="foto-evidencia" src={servico.urlFotoInicial} alt="Foto inicial do serviço" />
          </a>
          <p className="abreviado">Enviada. A foto inicial é imutável e não pode ser substituída.</p>
        </div>
      ) : (
        <div className="form-acoes">
          <Botao carregando={enviando} onClick={() => inputRef.current?.click()}>
            Enviar foto inicial
          </Botao>
          <input
            ref={inputRef}
            type="file"
            accept="image/jpeg,image/png,.jpg,.jpeg,.png"
            hidden
            onChange={(e) => void aoSelecionar(e.target.files)}
          />
          <span className="upload-ajuda">JPG, JPEG ou PNG · pré-requisito para confirmar a chegada</span>
        </div>
      )}
    </div>
  )
}
