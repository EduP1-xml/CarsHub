import { useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { atualizarLocalizacao, type Ator } from '../../api/perfis'
import { useToast } from '../../context/ToastContext'
import { obterPosicaoGps } from '../../hooks/useGeolocalizacao'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * F8/F35 — localização do perfil.
 * Entusiasta: PUT aceita UM dos dois campos (204 sem corpo).
 * Prestadores: exige latitude E longitude juntos (400 "Informe latitude e longitude para atualizar a localização.").
 * O organizador NÃO tem esta rota (aba oculta).
 */
export function AbaLocalizacao({ ator }: { ator: Ator }) {
  const { toast } = useToast()
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [buscandoGps, setBuscandoGps] = useState(false)

  const ehPrestador = ator === 'mecanicos' || ator === 'guinchos'

  async function usarGps() {
    setMensagem(null)
    setBuscandoGps(true)
    try {
      const posicao = await obterPosicaoGps()
      setLatitude(String(posicao.latitude))
      setLongitude(String(posicao.longitude))
    } catch (erro) {
      setMensagem(erro instanceof Error ? erro.message : mensagemDoErro(erro))
    } finally {
      setBuscandoGps(false)
    }
  }

  async function aoSalvar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)

    const temLat = latitude.trim() !== ''
    const temLong = longitude.trim() !== ''

    if (ehPrestador && (!temLat || !temLong)) {
      setMensagem('Informe latitude e longitude para atualizar a localização.')
      return
    }
    if (!ehPrestador && !temLat && !temLong) {
      setMensagem('Informe ao menos latitude ou longitude.')
      return
    }

    setEnviando(true)
    try {
      await atualizarLocalizacao(ator, {
        latitude: temLat ? Number(latitude) : null,
        longitude: temLong ? Number(longitude) : null,
      })
      toast('sucesso', 'Localização atualizada com sucesso.') // 204 sem corpo — confirmação local
    } catch (erro) {
      const normalizado = extrairErro(erro)
      setMensagem(mensagemDoErro(erro))
      void normalizado
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="form-secao">
      <div className="help-bloco">
        Sua localização é usada como <strong>último recurso</strong> ao solicitar serviços (GPS &gt; endereço digitado
        &gt; perfil) e como centro padrão do painel de chamados dos prestadores.
      </div>
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <form className="form-grid" onSubmit={aoSalvar}>
        <CampoTexto
          id="latitude"
          rotulo="Latitude"
          type="number"
          step="any"
          placeholder="-23.5505200"
          value={latitude}
          onChange={(e) => setLatitude(e.target.value)}
        />
        <CampoTexto
          id="longitude"
          rotulo="Longitude"
          type="number"
          step="any"
          placeholder="-46.6333100"
          value={longitude}
          onChange={(e) => setLongitude(e.target.value)}
        />
        <div className="campo-largo form-acoes">
          <Botao variante="secundario" carregando={buscandoGps} onClick={usarGps}>
            Usar meu GPS
          </Botao>
          <Botao type="submit" carregando={enviando}>
            Salvar localização
          </Botao>
        </div>
      </form>
    </div>
  )
}
