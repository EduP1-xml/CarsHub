import { useRef, useState } from 'react'
import { validarImagemLocal } from '../api/client'
import { Alerta } from './Alerta'

interface UploadImagensProps {
  /** Máximo de arquivos aceitos (2 para carro, 3 para evento, 1 para fotos avulsas). */
  maximo: number
  /** Lista de arquivos escolhidos (não enviados ainda). */
  arquivos: File[]
  aoMudar: (arquivos: File[]) => void
  /** URLs já existentes (ex.: fotos atuais do carro/evento) — exibidas como miniaturas. */
  urlsAtuais?: string[]
  /** true = somente um arquivo (foto-perfil, foto-inicial, foto-final). */
  unico?: boolean
}

/**
 * Seletor de imagens com pré-validação local (espelho de UX — a regra final é o backend:
 * 415 formato, 413 tamanho > 100MB, 400 nome com ".." e limites por rota).
 */
export function UploadImagens({ maximo, arquivos, aoMudar, urlsAtuais = [], unico = false }: UploadImagensProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [erroLocal, setErroLocal] = useState<string | null>(null)

  function aoSelecionar(lista: FileList | null) {
    if (!lista) return
    const novos = Array.from(lista)
    setErroLocal(null)

    const juntos = unico ? novos : [...arquivos, ...novos]
    if (juntos.length > maximo) {
      setErroLocal(`É permitido enviar no máximo ${maximo} foto(s).`)
      return
    }
    for (const arquivo of novos) {
      const invalido = validarImagemLocal(arquivo)
      if (invalido) {
        setErroLocal(invalido)
        return
      }
    }
    aoMudar(juntos)
    if (inputRef.current) inputRef.current.value = ''
  }

  function remover(index: number) {
    aoMudar(arquivos.filter((_, i) => i !== index))
  }

  return (
    <div className="upload">
      <div className="upload-acoes">
        <button
          type="button"
          className="botao botao-secundario"
          onClick={() => inputRef.current?.click()}
          disabled={!unico && arquivos.length >= maximo}
        >
          {unico ? 'Escolher imagem' : `Adicionar foto (${arquivos.length}/${maximo})`}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,.jpg,.jpeg,.png"
          multiple={!unico}
          hidden
          onChange={(e) => aoSelecionar(e.target.files)}
        />
        <span className="upload-ajuda">JPG, JPEG ou PNG · até 100MB por arquivo</span>
      </div>
      {erroLocal && <Alerta tipo="aviso">{erroLocal}</Alerta>}

      {(urlsAtuais.length > 0 || arquivos.length > 0) && (
        <div className="upload-miniaturas">
          {urlsAtuais.map((url, i) => (
            <figure key={`atual-${i}`} className="upload-miniatura">
              <img src={url} alt={`Foto atual ${i + 1}`} />
            </figure>
          ))}
          {arquivos.map((arquivo, i) => (
            <figure key={`novo-${i}`} className="upload-miniatura">
              <img src={URL.createObjectURL(arquivo)} alt={`Nova foto ${i + 1}`} />
              <button type="button" className="upload-remover" onClick={() => remover(i)} aria-label="Remover foto">
                ×
              </button>
            </figure>
          ))}
        </div>
      )}
    </div>
  )
}
