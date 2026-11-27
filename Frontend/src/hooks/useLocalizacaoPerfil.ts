// Localização salva no perfil (GET /{ator}/perfil → latitude/longitude [+ endereço do colecionador]).
// "possuiLocalizacao" = as duas coordenadas diferentes de null: só então o
// pop-up "mesmo endereço?" faz sentido (há algo para reaproveitar).
import { useCallback, useEffect, useState } from 'react'
import { buscarPerfil, type Ator } from '../api/perfis'

export interface LocalizacaoPerfil {
  latitude: number | null
  longitude: number | null
  /** Só o colecionador/entusiasta tem endereço salvo; demais = null. */
  enderecoFormatado: string | null
}

const SEM_LOCALIZACAO: LocalizacaoPerfil = { latitude: null, longitude: null, enderecoFormatado: null }

export function useLocalizacaoPerfil(ator: Ator) {
  const [localizacao, setLocalizacao] = useState<LocalizacaoPerfil>(SEM_LOCALIZACAO)

  const recarregar = useCallback(() => {
    buscarPerfil(ator)
      .then((resposta) =>
        // "?? null": campo ausente (ex.: backend desatualizado) vira null, nunca undefined.
        setLocalizacao({
          latitude: resposta.data.latitude ?? null,
          longitude: resposta.data.longitude ?? null,
          enderecoFormatado: resposta.data.enderecoFormatado ?? null,
        }),
      )
      // Falha ao ler o perfil = trata como "sem localização": o fluxo segue normal, sem pop-up.
      .catch(() => setLocalizacao(SEM_LOCALIZACAO))
  }, [ator])

  useEffect(() => {
    recarregar()
  }, [recarregar])

  const possuiLocalizacao = localizacao.latitude !== null && localizacao.longitude !== null

  return { ...localizacao, possuiLocalizacao, recarregar }
}
