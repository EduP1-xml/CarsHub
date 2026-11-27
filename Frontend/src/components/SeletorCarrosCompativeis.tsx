import { useEffect, useState } from 'react'
import { listarMeusCarrosCompativeis } from '../api/clubes'
import type { CarroCompativelDTO } from '../types/api'
import { mensagemDoErro } from '../utils/erros'
import { Alerta } from './Alerta'
import { Loading } from './Loading'

interface SeletorCarrosCompativeisProps {
  idClube: string
  /** Ids marcados (controlado pelo pai). */
  selecionados: string[]
  aoMudar: (ids: string[]) => void
  /** Chamado uma vez com a lista carregada (o pai inicializa a seleção com os já vinculados). */
  aoCarregar?: (carros: CarroCompativelDTO[]) => void
}

/**
 * Carros da MINHA garagem que atendem ao gosto do clube, com checkbox.
 * Usado no "Entrar" (colecionador) e em "Gerenciar meus carros".
 */
export function SeletorCarrosCompativeis({ idClube, selecionados, aoMudar, aoCarregar }: SeletorCarrosCompativeisProps) {
  const [carros, setCarros] = useState<CarroCompativelDTO[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let cancelado = false
    listarMeusCarrosCompativeis(idClube)
      .then((resposta) => {
        if (cancelado) return
        setCarros(resposta.data)
        aoCarregar?.(resposta.data)
      })
      .catch((e) => {
        if (!cancelado) setErro(mensagemDoErro(e))
      })
    return () => {
      cancelado = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idClube])

  if (erro) return <Alerta tipo="erro">{erro}</Alerta>
  if (carros === null) return <Loading rotulo="Buscando carros compatíveis..." />
  if (carros.length === 0) {
    return <p className="abreviado">Nenhum carro da sua garagem atende ao gosto deste clube.</p>
  }

  return (
    <ul className="seletor-carros">
      {carros.map((carro) => {
        const marcado = selecionados.includes(carro.idCarro)
        return (
          <li key={carro.idCarro}>
            <label className={`seletor-carro${marcado ? ' seletor-carro-marcado' : ''}`}>
              <input
                type="checkbox"
                checked={marcado}
                onChange={(e) =>
                  aoMudar(
                    e.target.checked
                      ? [...selecionados, carro.idCarro]
                      : selecionados.filter((id) => id !== carro.idCarro),
                  )
                }
              />
              {carro.fotoCapa ? (
                <img src={carro.fotoCapa} alt={`${carro.marca} ${carro.modelo}`} />
              ) : (
                <span className="seletor-carro-sem-foto" aria-hidden="true">
                  🚗
                </span>
              )}
              <span>
                <strong>
                  {carro.marca} {carro.modelo}
                </strong>
                <br />
                <span className="abreviado">
                  {carro.ano} · {carro.cor}
                </span>
              </span>
            </label>
          </li>
        )
      })}
    </ul>
  )
}
