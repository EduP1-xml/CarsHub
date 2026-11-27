import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect, CampoTexto } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { listarCarros } from '../../api/carros'
import { solicitarServicoGuincho, solicitarServicoMecanico } from '../../api/servicos'
import { useToast } from '../../context/ToastContext'
import { ModalMesmaLocalizacao } from '../../components/ModalMesmaLocalizacao'
import { obterPosicaoGps } from '../../hooks/useGeolocalizacao'
import { useLocalizacaoPerfil } from '../../hooks/useLocalizacaoPerfil'
import type { CarroRespostaDTO, ServicosOfertados } from '../../types/api'
import { lerCoordenada, propsCampoCoordenada, validarParCoordenadas } from '../../utils/coordenadas'
import { LABEL_SERVICOS_OFERTADOS, OPCOES_SERVICOS_OFERTADOS } from '../../utils/enums'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

interface NovoServicoProps {
  /** 'mecanico' | 'guincho' — o tipo do chamado desta tela. */
  tipo: 'mecanico' | 'guincho'
}

/**
 * F14/F15 — solicitar serviço. Prioridade de localização (R9):
 * GPS (lat+long JUNTAS; uma só → 400) > endereço digitado (geocodificação bloqueante) > perfil > 422.
 * Qualquer origem ATUALIZA a localização (e o endereço) do perfil no backend.
 * Pop-up "mesmo endereço?" (botão GPS / envio com endereço, só se o perfil tem localização):
 * SIM → envia sem localização (backend usa o perfil, sem gastar cota); NÃO → fluxo normal.
 * Sem garagem → 422; duplicata → 409; cooldown 5 min → 422;
 * quantidadeMecanicosElegiveis = 0 → aviso "nenhum mecânico compatível agora".
 */
export function NovoServico({ tipo }: NovoServicoProps) {
  const navigate = useNavigate()
  const { toast } = useToast()

  const ehMecanico = tipo === 'mecanico'

  const [carros, setCarros] = useState<CarroRespostaDTO[]>([])
  const [carregando, setCarregando] = useState(true)
  const [idCarro, setIdCarro] = useState('')
  const [servicos, setServicos] = useState<ServicosOfertados[]>([])
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [endereco, setEndereco] = useState('')
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [buscandoGps, setBuscandoGps] = useState(false)
  // Pop-up "mesmo endereço?": de onde veio a pergunta (botão GPS ou envio com endereço).
  const [perguntaPendente, setPerguntaPendente] = useState<'gps' | 'endereco' | null>(null)
  const [usandoPerfil, setUsandoPerfil] = useState(false)
  const perfil = useLocalizacaoPerfil('entusiastas')

  useEffect(() => {
    listarCarros(0, 100)
      .then((resposta) => {
        setCarros(resposta.data.conteudo)
        if (resposta.data.conteudo.length === 1) setIdCarro(resposta.data.conteudo[0].idCarro)
      })
      .catch((erro) => setMensagem(mensagemDoErro(erro)))
      .finally(() => setCarregando(false))
  }, [])

  async function executarGps() {
    setMensagem(null)
    setBuscandoGps(true)
    try {
      const posicao = await obterPosicaoGps()
      setLatitude(String(posicao.latitude))
      setLongitude(String(posicao.longitude))
      setEndereco('')
      setUsandoPerfil(false)
    } catch (erro) {
      setMensagem(erro instanceof Error ? erro.message : mensagemDoErro(erro))
    } finally {
      setBuscandoGps(false)
    }
  }

  /** "Usar meu GPS": se o perfil já tem localização, pergunta antes (economia de cota). */
  function usarGps() {
    if (perfil.possuiLocalizacao) {
      setPerguntaPendente('gps')
      return
    }
    void executarGps()
  }

  function aoResponderSim() {
    const origem = perguntaPendente
    setPerguntaPendente(null)
    // SIM: limpa GPS/endereço → o backend usa a localização (e o endereço) salvos no perfil.
    setLatitude('')
    setLongitude('')
    setEndereco('')
    setUsandoPerfil(true)
    if (origem === 'endereco') void enviar(null)
  }

  function aoResponderNao() {
    const origem = perguntaPendente
    setPerguntaPendente(null)
    if (origem === 'gps') void executarGps()
    if (origem === 'endereco') void enviar(endereco.trim())
  }

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)

    if (!idCarro) {
      setMensagem('Selecione o veículo da garagem.')
      return
    }
    if (ehMecanico && servicos.length === 0) {
      setMensagem('Informe pelo menos um serviço a ser realizado no veículo.')
      return
    }
    // Espelho da regra de GPS: lat/long devem vir JUNTAS (uma só → 400 do backend).
    const temLat = latitude.trim() !== ''
    const temLong = longitude.trim() !== ''
    if (temLat !== temLong) {
      setMensagem('Informe latitude e longitude juntas: o par de coordenadas é obrigatório.')
      return
    }
    const erroCoordenada = validarParCoordenadas(latitude, longitude)
    if (erroCoordenada) {
      setMensagem(erroCoordenada)
      return
    }

    // Endereço digitado (geolocalização imputada) gasta cota: pergunta antes, se houver perfil.
    const enderecoDigitado = endereco.trim()
    if (!temLat && enderecoDigitado && perfil.possuiLocalizacao) {
      setPerguntaPendente('endereco')
      return
    }

    await enviar(enderecoDigitado || null)
  }

  async function enviar(enderecoParaEnviar: string | null) {
    setMensagem(null)
    setEnviando(true)
    try {
      const resposta = ehMecanico
        ? await solicitarServicoMecanico({
            idCarro,
            servicosOfertados: servicos,
            latitude: lerCoordenada(latitude),
            longitude: lerCoordenada(longitude),
            endereco: enderecoParaEnviar,
          })
        : await solicitarServicoGuincho({
            idCarro,
            latitude: lerCoordenada(latitude),
            longitude: lerCoordenada(longitude),
            endereco: enderecoParaEnviar,
          })

      if (ehMecanico && resposta.data.quantidadeMecanicosElegiveis === 0) {
        toast('info', 'Solicitação criada, mas não há nenhum mecânico compatível no momento.')
      } else {
        toast('sucesso', `Solicitação criada (status: ${resposta.data.status}).`)
      }
      navigate(`/servicos/${ehMecanico ? 'mecanicos' : 'guinchos'}/${resposta.data.idServico}`)
    } catch (erro) {
      const normalizado = extrairErro(erro)
      setMensagem(mensagemDoErro(erro))
      void normalizado
    } finally {
      setEnviando(false)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando garagem..." />

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{ehMecanico ? 'Novo serviço mecânico' : 'Solicitar guincho'}</h1>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      {carros.length === 0 ? (
        <Alerta tipo="aviso">
          Apenas colecionadores podem solicitar serviços. Cadastre um veículo na sua garagem para começar.
          <div className="form-acoes">
            <Botao onClick={() => navigate('/garagem/novo')}>Cadastrar carro</Botao>
          </div>
        </Alerta>
      ) : (
        <form className="form-secao" onSubmit={aoEnviar}>
          <div className="form-grid">
            <CampoSelect
              id="idCarro"
              rotulo="Veículo"
              obrigatorio
              placeholder="Selecione o veículo..."
              opcoes={carros.map((carro) => ({
                valor: carro.idCarro,
                rotulo: `${carro.marca} ${carro.modelo} (${carro.placa})`,
              }))}
              value={idCarro}
              onChange={(e) => setIdCarro(e.target.value)}
            />

            {ehMecanico && (
              <div className="campo-largo">
                <span className="campo-rotulo">
                  Serviços (ao menos 1) <span className="campo-obrigatorio">*</span>
                </span>
                <div className="chips" style={{ marginTop: 6 }}>
                  {OPCOES_SERVICOS_OFERTADOS.map((servico) => {
                    const marcado = servicos.includes(servico)
                    return (
                      <label key={servico} className={`chip chip-clicavel${marcado ? ' chip-marcado' : ''}`}>
                        <input
                          type="checkbox"
                          checked={marcado}
                          onChange={() =>
                            setServicos(marcado ? servicos.filter((s) => s !== servico) : [...servicos, servico])
                          }
                          style={{ marginRight: 4 }}
                        />
                        {LABEL_SERVICOS_OFERTADOS[servico]}
                      </label>
                    )
                  })}
                </div>
              </div>
            )}
          </div>

          <h2>Localização da ocorrência</h2>
          <div className="help-bloco">
            Prioridade: <strong>GPS</strong> (latitude + longitude juntas) &gt; <strong>endereço digitado</strong>{' '}
            (geocodificação bloqueante) &gt; <strong>localização do perfil</strong>. A localização usada ATUALIZA a
            do seu perfil. Se nada for informado, o backend usa a localização do seu perfil.
          </div>
          {usandoPerfil && (
            <Alerta tipo="info">
              {perfil.enderecoFormatado ? (
                <>
                  Será usada a localização salva no seu perfil: <strong>{perfil.enderecoFormatado}</strong>
                </>
              ) : (
                'Será usada a localização salva no seu perfil.'
              )}
            </Alerta>
          )}
          <div className="form-grid">
            <CampoTexto
              id="latitude"
              rotulo="Latitude (GPS)"
              {...propsCampoCoordenada('latitude')}
              placeholder="-23.5505200"
              value={latitude}
              onChange={(e) => {
                setLatitude(e.target.value)
                setEndereco('')
                setUsandoPerfil(false)
              }}
            />
            <CampoTexto
              id="longitude"
              rotulo="Longitude (GPS)"
              {...propsCampoCoordenada('longitude')}
              placeholder="-46.6333100"
              value={longitude}
              onChange={(e) => {
                setLongitude(e.target.value)
                setEndereco('')
                setUsandoPerfil(false)
              }}
            />
            <div className="campo-largo">
              <CampoTexto
                id="endereco"
                rotulo="Ou endereço da ocorrência"
                placeholder="Rua, número - bairro, cidade - UF"
                value={endereco}
                onChange={(e) => {
                  setEndereco(e.target.value)
                  setLatitude('')
                  setLongitude('')
                  setUsandoPerfil(false)
                }}
              />
            </div>
          </div>
          <div className="form-acoes">
            <Botao variante="secundario" carregando={buscandoGps} onClick={usarGps}>
              Usar meu GPS
            </Botao>
            <Botao type="submit" carregando={enviando}>
              {ehMecanico ? 'Solicitar serviço' : 'Solicitar guincho'}
            </Botao>
            <Botao variante="texto" onClick={() => navigate('/servicos')}>
              Cancelar
            </Botao>
          </div>
        </form>
      )}

      <ModalMesmaLocalizacao
        aberto={perguntaPendente !== null}
        aoResponderSim={aoResponderSim}
        aoResponderNao={aoResponderNao}
        aoFechar={() => setPerguntaPendente(null)}
      />
    </div>
  )
}
