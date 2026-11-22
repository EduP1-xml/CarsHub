import { useState } from 'react'
import { Alerta } from '../Alerta'
import { Botao } from '../Botao'
import { CampoArea } from '../FormField'
import { Estrelas } from '../Estrelas'
import type { ServicoRespostaDTO } from '../../types/api'
import { formatarDataHora } from '../../utils/formatadores'

interface SecaoAvaliacaoProps {
  servico: ServicoRespostaDTO
  /** 'entusiasta' (avalia o prestador) ou 'prestador' (avalia o colecionador). */
  lado: 'entusiasta' | 'prestador'
  rotuloAlvo: string
  aoEnviar: (nota: number, comentario: string | null) => Promise<void>
  enviando: boolean
  mensagem: string | null
}

/**
 * F22/F42 — avaliação 1x por lado, imutável: nota 1..5 obrigatória + comentário
 * OPCIONAL (máx 500). Exige a própria foto final (CHEGOU).
 */
export function SecaoAvaliacao({ servico, lado, rotuloAlvo, aoEnviar, enviando, mensagem }: SecaoAvaliacaoProps) {
  const [nota, setNota] = useState(0)
  const [comentario, setComentario] = useState('')

  const minhaNota = lado === 'entusiasta' ? servico.notaAvaliacaoEntusiasta : servico.notaAvaliacaoPrestador
  const meuComentario =
    lado === 'entusiasta' ? servico.comentarioAvaliacaoEntusiasta : servico.comentarioAvaliacaoPrestador
  const minhaData = lado === 'entusiasta' ? servico.dataHoraAvaliacaoEntusiasta : servico.dataHoraAvaliacaoPrestador
  const minhaFoto = lado === 'entusiasta' ? servico.urlFotoFinalEntusiasta : servico.urlFotoFinalPrestador

  async function aoSubmeter() {
    if (nota < 1 || nota > 5) return
    await aoEnviar(nota, comentario.trim() || null)
    setComentario('')
    setNota(0)
  }

  return (
    <div className="detalhe-secao">
      <h2>Avaliar o {rotuloAlvo}</h2>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      {minhaNota !== null ? (
        <div>
          <Estrelas nota={minhaNota} rotulo={`Sua nota para o ${rotuloAlvo}`} />
          {meuComentario && <p>"{meuComentario}"</p>}
          <p className="abreviado">Avaliação enviada em {formatarDataHora(minhaData)} — é imutável.</p>
        </div>
      ) : minhaFoto ? (
        <div className="form-grid">
          <div className="campo-largo">
            <span className="campo-rotulo">Sua nota (1..5)</span>
            <Estrelas nota={nota} aoEscolher={setNota} rotulo="Escolha a nota" />
          </div>
          <CampoArea
            id="comentario"
            rotulo={`Comentário (opcional, máx 500)`}
            maxLength={500}
            rows={3}
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            ajuda={`${comentario.length}/500`}
          />
          <div className="campo-largo form-acoes">
            <Botao carregando={enviando} disabled={nota < 1} onClick={() => void aoSubmeter()}>
              Enviar avaliação
            </Botao>
          </div>
        </div>
      ) : (
        <p className="abreviado">Envie a sua foto final antes de avaliar o {rotuloAlvo}.</p>
      )}
    </div>
  )
}
