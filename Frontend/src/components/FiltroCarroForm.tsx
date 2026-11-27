// Formulário do filtro de carro (clube e evento). Cada característica tem um checkbox
// "usar este critério"; desmarcado = qualquer valor passa. Ao marcar:
//   enum → checkboxes dos valores aceitos · texto → lista com "+" · número → mínimo/máximo
//   som → um único checkbox "Exige som".
import type { EstadoFiltro, ItemFiltro } from './filtroCarro'
import { SECOES_FILTRO } from './filtroCarro'

interface FiltroCarroFormProps {
  valor: EstadoFiltro
  aoMudar: (proximo: EstadoFiltro) => void
  /** Prefixo dos ids dos inputs (para não colidir quando houver dois formulários). */
  idBase?: string
}

export function FiltroCarroForm({ valor, aoMudar, idBase = 'filtro' }: FiltroCarroFormProps) {
  function alternarAtivo(chave: keyof EstadoFiltro['ativos'], ativo: boolean) {
    const proximo: EstadoFiltro = { ...valor, ativos: { ...valor.ativos, [chave]: ativo } }
    // Texto marcado sem nenhuma linha: abre uma linha vazia para digitar.
    if (ativo && chave in valor.textos && valor.textos[chave as keyof EstadoFiltro['textos']].length === 0) {
      proximo.textos = { ...valor.textos, [chave]: [''] }
    }
    aoMudar(proximo)
  }

  function renderizarCorpo(item: Exclude<ItemFiltro, { tipo: 'som' }>) {
    const id = `${idBase}-${item.chave}`

    if (item.tipo === 'enum') {
      const marcados = valor.enums[item.chave]
      return (
        <div className="filtro-opcoes" role="group" aria-label={`${item.rotulo} aceitos`}>
          {Object.entries(item.labels).map(([valorEnum, rotulo]) => {
            const marcado = marcados.includes(valorEnum)
            return (
              <label key={valorEnum} className={`chip chip-clicavel${marcado ? ' chip-marcado' : ''}`}>
                <input
                  type="checkbox"
                  className="filtro-checkbox"
                  checked={marcado}
                  onChange={(e) =>
                    aoMudar({
                      ...valor,
                      enums: {
                        ...valor.enums,
                        [item.chave]: e.target.checked
                          ? [...marcados, valorEnum]
                          : marcados.filter((v) => v !== valorEnum),
                      },
                    })
                  }
                />
                {rotulo}
              </label>
            )
          })}
        </div>
      )
    }

    if (item.tipo === 'texto') {
      const linhas = valor.textos[item.chave]
      const atualizar = (novas: string[]) => aoMudar({ ...valor, textos: { ...valor.textos, [item.chave]: novas } })
      return (
        <div className="filtro-lista-texto">
          {linhas.map((linha, indice) => (
            <div key={indice} className="filtro-linha-texto">
              <input
                id={indice === 0 ? id : `${id}-${indice}`}
                className="campo-input"
                maxLength={item.maxLength}
                placeholder={`${item.rotulo} aceito(a)`}
                value={linha}
                onChange={(e) => atualizar(linhas.map((v, i) => (i === indice ? e.target.value : v)))}
              />
              <button
                type="button"
                className="botao botao-texto"
                aria-label={`Remover ${item.rotulo.toLowerCase()} ${indice + 1}`}
                title="Remover"
                onClick={() => atualizar(linhas.filter((_, i) => i !== indice))}
              >
                ×
              </button>
            </div>
          ))}
          <button
            type="button"
            className="botao botao-secundario filtro-adicionar"
            onClick={() => atualizar([...linhas, ''])}
            disabled={linhas.length >= 20}
          >
            + Adicionar {item.rotulo.toLowerCase()}
          </button>
        </div>
      )
    }

    const faixa = valor.faixas[item.chave]
    const atualizar = (campo: 'min' | 'max', texto: string) =>
      aoMudar({ ...valor, faixas: { ...valor.faixas, [item.chave]: { ...faixa, [campo]: texto } } })
    return (
      <div className="filtro-faixa">
        <label className="campo" htmlFor={`${id}-min`}>
          <span className="campo-rotulo">Mínimo</span>
          <input
            id={`${id}-min`}
            type="number"
            className="campo-input"
            min={item.min}
            max={item.max}
            step={item.step ?? 1}
            value={faixa.min}
            onChange={(e) => atualizar('min', e.target.value)}
          />
        </label>
        <label className="campo" htmlFor={`${id}-max`}>
          <span className="campo-rotulo">Máximo</span>
          <input
            id={`${id}-max`}
            type="number"
            className="campo-input"
            min={item.min}
            max={item.max}
            step={item.step ?? 1}
            value={faixa.max}
            onChange={(e) => atualizar('max', e.target.value)}
          />
        </label>
        <span className="campo-ajuda">Deixe um lado vazio para não limitar.</span>
      </div>
    )
  }

  return (
    <div className="filtro-carro">
      {SECOES_FILTRO.map((secao) => (
        <fieldset key={secao.titulo} className="filtro-secao">
          <legend>{secao.titulo}</legend>
          {secao.itens.map((item) => {
            if (item.tipo === 'som') {
              return (
                <div key="som" className="filtro-criterio">
                  <label className="filtro-criterio-toggle">
                    <input
                      type="checkbox"
                      checked={valor.som}
                      onChange={(e) => aoMudar({ ...valor, som: e.target.checked })}
                    />
                    {item.rotulo}
                  </label>
                </div>
              )
            }
            const ativo = !!valor.ativos[item.chave]
            return (
              <div key={item.chave} className={`filtro-criterio${ativo ? ' filtro-criterio-ativo' : ''}`}>
                <label className="filtro-criterio-toggle">
                  <input type="checkbox" checked={ativo} onChange={(e) => alternarAtivo(item.chave, e.target.checked)} />
                  {item.rotulo}
                  {!ativo && <span className="campo-ajuda"> · qualquer valor</span>}
                </label>
                {ativo && renderizarCorpo(item)}
              </div>
            )
          })}
        </fieldset>
      ))}
    </div>
  )
}
