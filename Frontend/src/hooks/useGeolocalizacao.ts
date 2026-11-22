// GPS do dispositivo (HTML5 Geolocation) — usado nas telas de localização,
// abertura de chamado e painel de chamados disponíveis.
export interface PosicaoGps {
  latitude: number
  longitude: number
}

export function obterPosicaoGps(): Promise<PosicaoGps> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('Seu navegador não suporta geolocalização.'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (posicao) => {
        resolve({
          latitude: Number(posicao.coords.latitude.toFixed(7)),
          longitude: Number(posicao.coords.longitude.toFixed(7)),
        })
      },
      (erro) => {
        reject(new Error(`Não foi possível obter a sua localização (código ${erro.code}).`))
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    )
  })
}
