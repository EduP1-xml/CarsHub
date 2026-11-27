import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { ModalMesmaLocalizacao } from '../../components/ModalMesmaLocalizacao'
import { Paginacao } from '../../components/Paginacao'
import { BadgeNotaMatch } from '../../components/BadgeStatus'
import { enviarProposta, listarDisponiveis, type PrefixoChamado } from '../../api/servicos'
import { useToast } from '../../context/ToastContext'
import { obterPosicaoGps } from '../../hooks/useGeolocalizacao'
import { useLocalizacaoPerfil } from '../../hooks/useLocalizacaoPerfil'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { ServicoDisponivelDTO } from '../../types/api'
import { lerCoordenada, propsCampoCoordenada, validarParCoordenadas } from '../../utils/coordenadas'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDistancia, formatarMoeda } from '../../utils/formatadores'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * F36 — painel de chamados disponíveis (prestador). Origem da busca:
 * GPS > endereço > localização do perfil; raioKm default 50 (≤ 0 → 400).
 * GPS/endereço/coordenadas da busca e da proposta ATUALIZAM a localização do perfil.
 * Pop-up "mesmo endereço?" no botão GPS e na busca com endereço (só se o perfil tem localização).
 * Ordem distância asc (+nome) vem pronta — não reordenar.
 * NÃO expor telefone nem valor do chamado; notaMatch pode ser null (sem coordenadas).
 */
export function PainelDisponiveis({ prefixo }: { prefixo: PrefixoChamado }) {
  const navigate = useNavigate()
  const { toast } = useToast()

  const [raioKm, setRaioKm] = useState('50')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [endereco, setEndereco] = useState('')
  const [buscandoGps, setBuscandoGps] = useState(false)
  const [filtroVersao, setFiltroVersao] = useState(0)

  const { envelope, carregando, erro, irParaPagina, atualizar } = usePaginacao<ServicoDisponivelDTO>(
    (pagina, tamanho) =>
      listarDisponiveis(
        prefixo,
        {
          raioKm: raioKm.trim() ? Number(raioKm) : null,
          latitude: lerCoordenada(latitude),
          longitude: lerCoordenada(longitude),
          endereco: endereco.trim() || null,
        },
        pagina,
        tamanho,
      ),
    5,
    [prefixo, filtroVersao],
  )

  // Pop-up "mesmo endereço?": de onde veio a pergunta (botão GPS ou busca com endereço).
  const [perguntaPendente, setPerguntaPendente] = useState<'gps' | 'endereco' | null>(null)
  const [usandoPerfil, setUsandoPerfil] = useState(false)
  const perfil = useLocalizacaoPerfil(prefixo)
  const recarregarPerfil = perfil.recarregar

  // Cada busca (GPS/endereço) atualiza o perfil no backend: relê para o pop-up saber
  // se já existe localização salva.
  useEffect(() => {
    if (!carregando && !erro) recarregarPerfil()
  }, [carregando, erro, recarregarPerfil])

  // Proposta
  const [propondoPara, setPropondoPara] = useState<ServicoDisponivelDTO | null>(null)
  const [valorProposto, setValorProposto] = useState('')
  const [propostaLat, setPropostaLat] = useState('')
  const [propostaLong, setPropostaLong] = useState('')
  const [enviandoProposta, setEnviandoProposta] = useState(false)
  const [mensagemProposta, setMensagemProposta] = useState<string | null>(null)
  const [mensagemFiltro, setMensagemFiltro] = useState<string | null>(null)

  async function executarGps() {
    setMensagemFiltro(null)
    setBuscandoGps(true)
    try {
      const posicao = await obterPosicaoGps()
      setLatitude(String(posicao.latitude))
      setLongitude(String(posicao.longitude))
      setEndereco('')
      setUsandoPerfil(false)
    } catch (e) {
      setMensagemFiltro(e instanceof Error ? e.message : mensagemDoErro(e))
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

  /** "Buscar": valida coordenadas; endereço digitado + perfil com localização → pergunta antes. */
  function buscar() {
    setMensagemFiltro(null)
    const temLat = latitude.trim() !== ''
    const temLong = longitude.trim() !== ''
    if (temLat !== temLong) {
      setMensagemFiltro('Informe latitude e longitude juntas: o par de coordenadas é obrigatório.')
      return
    }
    const erroCoordenada = validarParCoordenadas(latitude, longitude)
    if (erroCoordenada) {
      setMensagemFiltro(erroCoordenada)
      return
    }
    if (!temLat && endereco.trim() && perfil.possuiLocalizacao) {
      setPerguntaPendente('endereco')
      return
    }
    setFiltroVersao((v) => v + 1)
  }

  function aoResponderSim() {
    const origem = perguntaPendente
    setPerguntaPendente(null)
    // SIM: limpa GPS/endereço → o backend usa a localização salva no perfil.
    setLatitude('')
    setLongitude('')
    setEndereco('')
    setUsandoPerfil(true)
    if (origem === 'endereco') setFiltroVersao((v) => v + 1)
  }

  function aoResponderNao() {
    const origem = perguntaPendente
    setPerguntaPendente(null)
    if (origem === 'gps') void executarGps()
    if (origem === 'endereco') setFiltroVersao((v) => v + 1)
  }

  async function aoEnviarProposta() {
    if (!propondoPara) return
    setMensagemProposta(null)
    const valor = Number(valorProposto)
    if (!Number.isFinite(valor) || valor <= 0) {
      setMensagemProposta('O valor da proposta deve ser maior que zero.')
      return
    }
    const temLat = propostaLat.trim() !== ''
    const temLong = propostaLong.trim() !== ''
    if (temLat !== temLong) {
      setMensagemProposta('Informe latitude e longitude juntas: o par de coordenadas é obrigatório.')
      return
    }
    const erroCoordenada = validarParCoordenadas(propostaLat, propostaLong)
    if (erroCoordenada) {
      setMensagemProposta(erroCoordenada)
      return
    }

    setEnviandoProposta(true)
    try {
      const resposta = await enviarProposta(prefixo, propondoPara.idServico, {
        valorProposto: valor,
        latitude: lerCoordenada(propostaLat),
        longitude: lerCoordenada(propostaLong),
      })
      toast('sucesso', `Proposta enviada! Status do chamado: ${resposta.data.status}.`)
      setPropondoPara(null)
      setValorProposto('')
      setPropostaLat('')
      setPropostaLong('')
      navigate(`/prestador/servicos/${propondoPara.idServico}`)
    } catch (e) {
      setMensagemProposta(mensagemDoErro(e))
      void extrairErro(e)
    } finally {
      setEnviandoProposta(false)
    }
  }

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Chamados disponíveis</h1>
      </div>

      <div className="form-secao">
        <div className="help-bloco">
          Origem da busca: <strong>GPS</strong> &gt; <strong>endereço digitado</strong> &gt;{' '}
          <strong>localização do perfil</strong>. A origem usada ATUALIZA a localização do seu perfil. Raio padrão: 50
          km.
        </div>
        {mensagemFiltro && <Alerta tipo="erro">{mensagemFiltro}</Alerta>}
        {usandoPerfil && <Alerta tipo="info">Será usada a localização salva no seu perfil.</Alerta>}
        <div className="form-grid">
          <CampoTexto
            id="raioKm"
            rotulo="Raio (km)"
            type="number"
            step="any"
            value={raioKm}
            onChange={(e) => setRaioKm(e.target.value)}
          />
          <CampoTexto
            id="latitude"
            rotulo="Latitude (GPS)"
            {...propsCampoCoordenada('latitude')}
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
            value={longitude}
            onChange={(e) => {
              setLongitude(e.target.value)
              setEndereco('')
              setUsandoPerfil(false)
            }}
          />
          <CampoTexto
            id="endereco"
            rotulo="Ou endereço"
            value={endereco}
            placeholder="Rua, número - bairro, cidade - UF"
            onChange={(e) => {
              setEndereco(e.target.value)
              setLatitude('')
              setLongitude('')
              setUsandoPerfil(false)
            }}
          />
          <div className="campo-largo form-acoes">
            <Botao variante="secundario" carregando={buscandoGps} onClick={usarGps}>
              Usar meu GPS
            </Botao>
            <Botao onClick={buscar}>Buscar</Botao>
          </div>
        </div>
      </div>

      {carregando && <LoadingPagina rotulo="Buscando chamados..." />}
      {erro && (
        <Alerta tipo="erro">
          {mensagemDoErro(erro)}{' '}
          <button type="button" className="botao botao-texto" onClick={atualizar}>
            Tentar novamente
          </button>
        </Alerta>
      )}

      {!carregando && !erro && envelope.conteudo.length === 0 && (
        <EstadoVazio
          titulo="Nenhum chamado na região"
          descricao="Não há chamados elegíveis para você neste raio agora."
        />
      )}

      {envelope.conteudo.map((item) => (
        <article key={item.idServico} className="card">
          <div className="card-cabecalho">
            <div>
              <h2 className="card-titulo">{item.carroDescricao}</h2>
              <p className="card-subtitulo">Colecionador: {item.nomeEntusiasta}</p>
            </div>
            <BadgeNotaMatch notaMatch={item.notaMatch} />
          </div>
          <div className="card-corpo">
            <div className="linha-dados">
              <span className="rotulo">Distância</span>
              <span>{formatarDistancia(item.distanciaKm)}</span>
            </div>
            {item.servicosSolicitados.length > 0 && (
              <div className="chips">
                {item.servicosSolicitados.map((servico) => (
                  <span key={servico} className="chip">
                    {LABEL_SERVICOS_OFERTADOS[servico]}
                  </span>
                ))}
              </div>
            )}
          </div>
          <div className="card-acoes">
            <Botao onClick={() => setPropondoPara(item)}>Enviar proposta</Botao>
          </div>
        </article>
      ))}

      <Paginacao envelope={envelope} aoMudarPagina={irParaPagina} />

      <Modal
        aberto={propondoPara !== null}
        titulo={`Proposta para ${propondoPara?.carroDescricao ?? ''}`}
        aoFechar={() => setPropondoPara(null)}
      >
        {mensagemProposta && <Alerta tipo="erro">{mensagemProposta}</Alerta>}
        <div className="form-grid">
          <CampoTexto
            id="valorProposto"
            rotulo="Valor da proposta (R$)"
            obrigatorio
            type="number"
            step="0.01"
            min="0.01"
            value={valorProposto}
            onChange={(e) => setValorProposto(e.target.value)}
            ajuda="Máx. 8 dígitos inteiros e 2 decimais"
          />
          <CampoTexto
            id="propostaLat"
            rotulo="Sua latitude (opcional)"
            {...propsCampoCoordenada('latitude')}
            value={propostaLat}
            onChange={(e) => setPropostaLat(e.target.value)}
            ajuda="Se informadas, atualizam a localização do seu perfil"
          />
          <CampoTexto
            id="propostaLong"
            rotulo="Sua longitude (opcional)"
            {...propsCampoCoordenada('longitude')}
            value={propostaLong}
            onChange={(e) => setPropostaLong(e.target.value)}
          />
        </div>
        <div className="form-acoes">
          <Botao carregando={enviandoProposta} onClick={() => void aoEnviarProposta()}>
            Enviar proposta {valorProposto ? `(${formatarMoeda(Number(valorProposto))})` : ''}
          </Botao>
        </div>
      </Modal>

      <ModalMesmaLocalizacao
        aberto={perguntaPendente !== null}
        aoResponderSim={aoResponderSim}
        aoResponderNao={aoResponderNao}
        aoFechar={() => setPerguntaPendente(null)}
      />
    </div>
  )
}
