// Carrossel leve de fotos — usado nos cards da garagem.
// As setas ficam invisíveis e só aparecem ao passar o mouse sobre a foto
// (ver estilos .carrossel / .carrossel-seta em global.css).
import { useEffect, useState } from 'react'

interface CarrosselFotosProps {
  fotos: string[]
  alt: string
  /** Opcional: clicar na foto abre a imagem original em outra aba (usado no detalhe). */
  abrirEmNovaAba?: boolean
}

/** Exibe uma foto por vez; setas e indicadores só aparecem com mais de uma foto. */
export function CarrosselFotos({ fotos, alt, abrirEmNovaAba = false }: CarrosselFotosProps) {
  const [indice, setIndice] = useState(0)

  // Se a lista de fotos mudar (recarga/paginação), volta para a primeira.
  useEffect(() => {
    setIndice(0)
  }, [fotos])

  if (fotos.length === 0) {
    return (
      <div className="carrossel carrossel-sem-fotos">
        <span>Sem fotos cadastradas</span>
      </div>
    )
  }

  const total = fotos.length
  const fotoAtual = fotos[Math.min(indice, total - 1)]
  const imagem = <img src={fotoAtual} alt={`${alt} — foto ${indice + 1} de ${total}`} />

  function irPara(novo: number) {
    setIndice((novo + total) % total)
  }

  return (
    <div
      className="carrossel"
      role="group"
      aria-roledescription="carrossel"
      aria-label={`Fotos de ${alt}`}
    >
      {abrirEmNovaAba ? (
        <a
          href={fotoAtual}
          target="_blank"
          rel="noreferrer"
          className="carrossel-abrir"
          title="Abrir foto em nova aba"
        >
          {imagem}
        </a>
      ) : (
        imagem
      )}

      {total > 1 && (
        <>
          <button
            type="button"
            className="carrossel-seta carrossel-seta-esquerda"
            onClick={() => irPara(indice - 1)}
            aria-label="Foto anterior"
          >
            ‹
          </button>
          <button
            type="button"
            className="carrossel-seta carrossel-seta-direita"
            onClick={() => irPara(indice + 1)}
            aria-label="Próxima foto"
          >
            ›
          </button>

          <div className="carrossel-indicadores">
            {fotos.map((_, indiceFoto) => (
              <button
                key={indiceFoto}
                type="button"
                className={`carrossel-ponto${indiceFoto === indice ? ' ativo' : ''}`}
                onClick={() => setIndice(indiceFoto)}
                aria-label={`Ir para a foto ${indiceFoto + 1}`}
                aria-current={indiceFoto === indice}
              />
            ))}
          </div>

          <span className="carrossel-contador">
            {indice + 1}/{total}
          </span>
        </>
      )}
    </div>
  )
}
