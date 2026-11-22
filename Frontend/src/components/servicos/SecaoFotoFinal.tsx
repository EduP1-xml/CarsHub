import { useRef, useState } from 'react'
import { Alerta } from '../Alerta'
import { Botao } from '../Botao'
import { validarImagemLocal } from '../../api/client'
import type { ServicoRespostaDTO } from '../../types/api'
import { mensagemDoErro } from '../../utils/erros'

interface SecaoFotoFinalProps {
  servico: ServicoRespostaDTO
  /** 'entusiasta' (colecionador) ou 'prestador'. */
  lado: 'entusiasta' | 'prestador'
  rotuloOutro: string
  aoEnviar: (arquivo: File) => Promise<void>
  enviando: boolean
  mensagem: string | null
}

/** F21/F41 — foto final: só em CHEGOU, UMA vez por lado. */
export function SecaoFotoFinal({ servico, lado, rotuloOutro, aoEnviar, enviando, mensagem }: SecaoFotoFinalProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [erroLocal, setErroLocal] = useState<string | null>(null)

  const minhaFoto = lado === 'entusiasta' ? servico.urlFotoFinalEntusiasta : servico.urlFotoFinalPrestador
  const outraFoto = lado === 'entusiasta' ? servico.urlFotoFinalPrestador : servico.urlFotoFinalEntusiasta

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
      <h2>Foto final do serviço</h2>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {erroLocal && <Alerta tipo="erro">{erroLocal}</Alerta>}

      <div className="linha-dados">
        <span className="rotulo">Sua foto final</span>
        {minhaFoto ? (
          <a href={minhaFoto} target="_blank" rel="noreferrer">
            <img className="foto-evidencia" src={minhaFoto} alt="Sua foto final" />
          </a>
        ) : (
          <span className="abreviado">
            Não enviada.{' '}
            <Botao variante="texto" carregando={enviando} onClick={() => inputRef.current?.click()}>
              Enviar foto final
            </Botao>
          </span>
        )}
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,.jpg,.jpeg,.png"
          hidden
          onChange={(e) => void aoSelecionar(e.target.files)}
        />
      </div>
      <div className="linha-dados">
        <span className="rotulo">Foto do {rotuloOutro}</span>
        {outraFoto ? (
          <a href={outraFoto} target="_blank" rel="noreferrer">
            <img className="foto-evidencia" src={outraFoto} alt={`Foto final do ${rotuloOutro}`} />
          </a>
        ) : (
          <span className="abreviado">Ainda não enviada pelo {rotuloOutro}.</span>
        )}
      </div>
    </div>
  )
}
