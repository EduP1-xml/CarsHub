// Configuração e conversões do filtro de carro (gosto do clube / carros aceitos no evento).
// O formulário trabalha com um ESTADO (critérios ativos + valores digitados); o JSON
// enviado ao backend é o FiltroCarroDTO montado por estadoParaFiltro.
import type { FiltroCarroDTO } from '../types/api'
import {
  LABEL_ALIMENTACAO,
  LABEL_CAMBIO,
  LABEL_CARROCERIA,
  LABEL_COMBUSTIVEL,
  LABEL_DIRECAO,
  LABEL_FREIO,
  LABEL_INJECAO,
  LABEL_MOTOR,
  LABEL_REVESTIMENTO,
  LABEL_RODA,
  LABEL_SUSPENSAO,
  LABEL_TRACAO,
} from '../utils/enums'

export type CriterioTexto = 'marcas' | 'modelos' | 'cores'
export type CriterioEnum =
  | 'carrocerias'
  | 'motores'
  | 'injecoes'
  | 'alimentacoes'
  | 'combustiveis'
  | 'cambios'
  | 'tracoes'
  | 'suspensoes'
  | 'freios'
  | 'direcoes'
  | 'rodas'
  | 'materiaisRevestimentoInterno'
export type CriterioFaixa = 'ano' | 'quantidadeCilindros' | 'litragemMotor' | 'aro' | 'numeroPortas' | 'numeroAssentos'

export type ItemFiltro =
  | { tipo: 'texto'; chave: CriterioTexto; rotulo: string; maxLength: number }
  | { tipo: 'enum'; chave: CriterioEnum; rotulo: string; labels: Record<string, string> }
  | { tipo: 'faixa'; chave: CriterioFaixa; rotulo: string; min: number; max: number; step?: number }
  | { tipo: 'som'; rotulo: string }

export interface SecaoFiltro {
  titulo: string
  itens: ItemFiltro[]
}

/** Seções do formulário (agrupadas para não virar um paredão de campos). */
export const SECOES_FILTRO: SecaoFiltro[] = [
  {
    titulo: 'Identificação',
    itens: [
      { tipo: 'texto', chave: 'marcas', rotulo: 'Marca', maxLength: 30 },
      { tipo: 'texto', chave: 'modelos', rotulo: 'Modelo', maxLength: 30 },
      { tipo: 'texto', chave: 'cores', rotulo: 'Cor', maxLength: 20 },
      { tipo: 'enum', chave: 'carrocerias', rotulo: 'Carroceria', labels: LABEL_CARROCERIA },
      { tipo: 'faixa', chave: 'ano', rotulo: 'Ano', min: 1886, max: 2100 },
    ],
  },
  {
    titulo: 'Motor',
    itens: [
      { tipo: 'enum', chave: 'motores', rotulo: 'Motor', labels: LABEL_MOTOR },
      { tipo: 'faixa', chave: 'quantidadeCilindros', rotulo: 'Cilindros', min: 1, max: 16 },
      { tipo: 'faixa', chave: 'litragemMotor', rotulo: 'Litragem (L)', min: 0.049, max: 15, step: 0.001 },
      { tipo: 'enum', chave: 'injecoes', rotulo: 'Injeção', labels: LABEL_INJECAO },
      { tipo: 'enum', chave: 'alimentacoes', rotulo: 'Alimentação', labels: LABEL_ALIMENTACAO },
      { tipo: 'enum', chave: 'combustiveis', rotulo: 'Combustível', labels: LABEL_COMBUSTIVEL },
    ],
  },
  {
    titulo: 'Transmissão e chassi',
    itens: [
      { tipo: 'enum', chave: 'cambios', rotulo: 'Câmbio', labels: LABEL_CAMBIO },
      { tipo: 'enum', chave: 'tracoes', rotulo: 'Tração', labels: LABEL_TRACAO },
      { tipo: 'enum', chave: 'suspensoes', rotulo: 'Suspensão', labels: LABEL_SUSPENSAO },
      { tipo: 'enum', chave: 'freios', rotulo: 'Freio', labels: LABEL_FREIO },
      { tipo: 'enum', chave: 'direcoes', rotulo: 'Direção', labels: LABEL_DIRECAO },
      { tipo: 'enum', chave: 'rodas', rotulo: 'Roda', labels: LABEL_RODA },
      { tipo: 'faixa', chave: 'aro', rotulo: 'Aro', min: 8, max: 32 },
    ],
  },
  {
    titulo: 'Interior e conforto',
    itens: [
      { tipo: 'enum', chave: 'materiaisRevestimentoInterno', rotulo: 'Revestimento interno', labels: LABEL_REVESTIMENTO },
      { tipo: 'faixa', chave: 'numeroPortas', rotulo: 'Portas', min: 1, max: 5 },
      { tipo: 'faixa', chave: 'numeroAssentos', rotulo: 'Assentos', min: 1, max: 50 },
      { tipo: 'som', rotulo: 'Exige som' },
    ],
  },
]

const TEXTOS: CriterioTexto[] = ['marcas', 'modelos', 'cores']
const ENUMS: CriterioEnum[] = [
  'carrocerias', 'motores', 'injecoes', 'alimentacoes', 'combustiveis', 'cambios',
  'tracoes', 'suspensoes', 'freios', 'direcoes', 'rodas', 'materiaisRevestimentoInterno',
]
const FAIXAS: CriterioFaixa[] = ['ano', 'quantidadeCilindros', 'litragemMotor', 'aro', 'numeroPortas', 'numeroAssentos']

/** Estado do formulário: o checkbox "usar este critério" + os valores de cada critério. */
export interface EstadoFiltro {
  ativos: Partial<Record<CriterioTexto | CriterioEnum | CriterioFaixa, boolean>>
  textos: Record<CriterioTexto, string[]>
  enums: Record<CriterioEnum, string[]>
  faixas: Record<CriterioFaixa, { min: string; max: string }>
  som: boolean
}

export function estadoVazio(): EstadoFiltro {
  return {
    ativos: {},
    textos: { marcas: [''], modelos: [''], cores: [''] },
    enums: Object.fromEntries(ENUMS.map((chave) => [chave, []])) as unknown as Record<CriterioEnum, string[]>,
    faixas: Object.fromEntries(FAIXAS.map((chave) => [chave, { min: '', max: '' }])) as unknown as Record<
      CriterioFaixa,
      { min: string; max: string }
    >,
    som: false,
  }
}

type Dto = Record<string, unknown>

/** FiltroCarroDTO (resposta do backend) → estado do formulário. */
export function filtroParaEstado(filtro: FiltroCarroDTO | null | undefined): EstadoFiltro {
  const estado = estadoVazio()
  if (!filtro) return estado
  const dto = filtro as Dto

  TEXTOS.forEach((chave) => {
    const valores = (dto[chave] as string[] | null | undefined) ?? []
    if (valores.length > 0) {
      estado.ativos[chave] = true
      estado.textos[chave] = [...valores]
    }
  })
  ENUMS.forEach((chave) => {
    const valores = (dto[chave] as string[] | null | undefined) ?? []
    if (valores.length > 0) {
      estado.ativos[chave] = true
      estado.enums[chave] = [...valores]
    }
  })
  FAIXAS.forEach((chave) => {
    const minimo = dto[`${chave}Min`] as number | null | undefined
    const maximo = dto[`${chave}Max`] as number | null | undefined
    if (minimo != null || maximo != null) {
      estado.ativos[chave] = true
      estado.faixas[chave] = { min: minimo != null ? String(minimo) : '', max: maximo != null ? String(maximo) : '' }
    }
  })
  estado.som = filtro.som === true
  return estado
}

function numeroOuNulo(texto: string): number | null {
  if (texto.trim() === '') return null
  const valor = Number(texto)
  return Number.isFinite(valor) ? valor : null
}

/** Estado do formulário → FiltroCarroDTO. Critério desmarcado vai vazio/nulo (= qualquer valor). */
export function estadoParaFiltro(estado: EstadoFiltro): FiltroCarroDTO {
  const dto: Dto = {}
  TEXTOS.forEach((chave) => {
    dto[chave] = estado.ativos[chave] ? estado.textos[chave].map((v) => v.trim()).filter((v) => v !== '') : []
  })
  ENUMS.forEach((chave) => {
    dto[chave] = estado.ativos[chave] ? [...estado.enums[chave]] : []
  })
  FAIXAS.forEach((chave) => {
    const ativo = estado.ativos[chave]
    dto[`${chave}Min`] = ativo ? numeroOuNulo(estado.faixas[chave].min) : null
    dto[`${chave}Max`] = ativo ? numeroOuNulo(estado.faixas[chave].max) : null
  })
  dto.som = estado.som ? true : null
  return dto as FiltroCarroDTO
}

/** true quando o filtro tem pelo menos uma característica marcada. */
export function possuiCriterio(filtro: FiltroCarroDTO | null | undefined): boolean {
  if (!filtro) return false
  const dto = filtro as Dto
  return (
    [...TEXTOS, ...ENUMS].some((chave) => ((dto[chave] as unknown[] | null | undefined) ?? []).length > 0) ||
    FAIXAS.some((chave) => dto[`${chave}Min`] != null || dto[`${chave}Max`] != null) ||
    filtro.som === true
  )
}

/** Faixas com mínimo > máximo (espelho de UX; a regra final é o 422 do backend). */
export function faixasInvertidas(filtro: FiltroCarroDTO): string[] {
  const dto = filtro as Dto
  const rotulos: Record<CriterioFaixa, string> = {
    ano: 'Ano', quantidadeCilindros: 'Cilindros', litragemMotor: 'Litragem', aro: 'Aro',
    numeroPortas: 'Portas', numeroAssentos: 'Assentos',
  }
  return FAIXAS.filter((chave) => {
    const minimo = dto[`${chave}Min`] as number | null
    const maximo = dto[`${chave}Max`] as number | null
    return minimo != null && maximo != null && minimo > maximo
  }).map((chave) => rotulos[chave])
}

/** Linhas legíveis do filtro ("Marca: Volkswagen, Ford", "Ano: 1960 a 1985"...). */
export function descreverFiltro(filtro: FiltroCarroDTO | null | undefined): string[] {
  if (!filtro) return []
  const dto = filtro as Dto
  const linhas: string[] = []

  SECOES_FILTRO.forEach((secao) =>
    secao.itens.forEach((item) => {
      if (item.tipo === 'texto') {
        const valores = (dto[item.chave] as string[] | null | undefined) ?? []
        if (valores.length > 0) linhas.push(`${item.rotulo}: ${valores.join(', ')}`)
      } else if (item.tipo === 'enum') {
        const valores = (dto[item.chave] as string[] | null | undefined) ?? []
        if (valores.length > 0) linhas.push(`${item.rotulo}: ${valores.map((v) => item.labels[v] ?? v).join(', ')}`)
      } else if (item.tipo === 'faixa') {
        const minimo = dto[`${item.chave}Min`] as number | null | undefined
        const maximo = dto[`${item.chave}Max`] as number | null | undefined
        if (minimo != null && maximo != null) linhas.push(`${item.rotulo}: ${minimo} a ${maximo}`)
        else if (minimo != null) linhas.push(`${item.rotulo}: a partir de ${minimo}`)
        else if (maximo != null) linhas.push(`${item.rotulo}: até ${maximo}`)
      } else if (filtro.som === true) {
        linhas.push('Exige som')
      }
    }),
  )
  return linhas
}
