import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoTexto } from '../../components/FormField'
import { EstadoVazio } from '../../components/EstadoVazio'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { Paginacao } from '../../components/Paginacao'
import { BadgeNotaMatch } from '../../components/BadgeStatus'
import { enviarProposta, listarDisponiveis, type PrefixoChamado } from '../../api/servicos'
import { useToast } from '../../context/ToastContext'
import { obterPosicaoGps } from '../../hooks/useGeolocalizacao'
import { usePaginacao } from '../../hooks/usePaginacao'
import type { ServicoDisponivelDTO } from '../../types/api'
import { LABEL_SERVICOS_OFERTADOS } from '../../utils/enums'
import { formatarDistancia, formatarMoeda } from '../../utils/formatadores'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * F36 — painel de chamados disponíveis (prestador). Origem da busca:
 * GPS > endereço > localização do perfil; raioKm default 50 (≤ 0 → 400).
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
          latitude: latitude.trim() ? Number(latitude) : null,
          longitude: longitude.trim() ? Number(longitude) : null,
          endereco: endereco.trim() || null,
        },
        pagina,
        tamanho,
      ),
    5,
    [prefixo, filtroVersao],
  )

  // Proposta
  const [propondoPara, setPropondoPara] = useState<ServicoDisponivelDTO | null>(null)
  const [valorProposto, setValorProposto] = useState('')
  const [propostaLat, setPropostaLat] = useState('')
  const [propostaLong, setPropostaLong] = useState('')
  const [enviandoProposta, setEnviandoProposta] = useState(false)
  const [mensagemProposta, setMensagemProposta] = useState<string | null>(null)
  const [mensagemFiltro, setMensagemFiltro] = useState<string | null>(null)

  async function usarGps() {
    setMensagemFiltro(null)
    setBuscandoGps(true)
    try {
      const posicao = await obterPosicaoGps()
      setLatitude(String(posicao.latitude))
      setLongitude(String(posicao.longitude))
      setEndereco('')
    } catch (e) {
      setMensagemFiltro(e instanceof Error ? e.message : mensagemDoErro(e))
    } finally {
      setBuscandoGps(false)
    }
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

    setEnviandoProposta(true)
    try {
      const resposta = await enviarProposta(prefixo, propondoPara.idServico, {
        valorProposto: valor,
        latitude: temLat ? Number(propostaLat) : null,
        longitude: temLong ? Number(propostaLong) : null,
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
          <strong>localização do perfil</strong>. Raio padrão: 50 km.
        </div>
        {mensagemFiltro && <Alerta tipo="erro">{mensagemFiltro}</Alerta>}
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
            type="number"
            step="any"
            value={latitude}
            onChange={(e) => {
              setLatitude(e.target.value)
              setEndereco('')
            }}
          />
          <CampoTexto
            id="longitude"
            rotulo="Longitude (GPS)"
            type="number"
            step="any"
            value={longitude}
            onChange={(e) => {
              setLongitude(e.target.value)
              setEndereco('')
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
            }}
          />
          <div className="campo-largo form-acoes">
            <Botao variante="secundario" carregando={buscandoGps} onClick={usarGps}>
              Usar meu GPS
            </Botao>
            <Botao onClick={() => setFiltroVersao((v) => v + 1)}>Buscar</Botao>
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
            type="number"
            step="any"
            value={propostaLat}
            onChange={(e) => setPropostaLat(e.target.value)}
          />
          <CampoTexto
            id="propostaLong"
            rotulo="Sua longitude (opcional)"
            type="number"
            step="any"
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
    </div>
  )
}
