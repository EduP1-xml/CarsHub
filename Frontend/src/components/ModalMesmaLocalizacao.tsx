import { Botao } from './Botao'

interface ModalMesmaLocalizacaoProps {
  aberto: boolean
  /** SIM → usar a localização do perfil (não gasta cota da Geocoding API). */
  aoResponderSim: () => void
  /** NÃO → seguir com o GPS ou o endereço digitado, normalmente. */
  aoResponderNao: () => void
  /** × do cabeçalho: fecha sem fazer nada. */
  aoFechar: () => void
}

/**
 * Pop-up de economia de cota: aparece ao usar o GPS ("Usar meu GPS") ou ao
 * enviar com endereço digitado, SOMENTE quando o perfil já tem localização.
 * Coordenadas digitadas à mão não abrem este pop-up (não gastam cota).
 */
export function ModalMesmaLocalizacao({ aberto, aoResponderSim, aoResponderNao, aoFechar }: ModalMesmaLocalizacaoProps) {
  if (!aberto) return null
  const titulo = 'Localização'
  return (
    <div className="modal-fundo" role="dialog" aria-modal="true" aria-label={titulo}>
      <div className="modal-caixa">
        <div className="modal-cabecalho">
          <h3>{titulo}</h3>
          <button type="button" className="modal-fechar" aria-label="Fechar" onClick={aoFechar}>
            ×
          </button>
        </div>
        <div className="modal-corpo">
          <p>Você está no mesmo endereço de quando usou a geolocalização pela última vez?</p>
        </div>
        <div className="modal-acoes">
          <Botao variante="secundario" onClick={aoResponderNao}>
            NÃO
          </Botao>
          <Botao onClick={aoResponderSim}>SIM</Botao>
        </div>
      </div>
    </div>
  )
}
