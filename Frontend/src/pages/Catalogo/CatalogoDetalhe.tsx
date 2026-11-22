import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect } from '../../components/FormField'
import { LoadingPagina } from '../../components/Loading'
import { detalharEventoPublico } from '../../api/eventos'
import { listarCarros } from '../../api/carros'
import { inscreverEmEvento } from '../../api/inscricoes'
import { ehColecionador, useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import type { CarroRespostaDTO, CatalogoEventoResponseDTO } from '../../types/api'
import { formatarCep, formatarDataHora, formatarMoeda } from '../../utils/formatadores'
import { extrairErro, mensagemDoErro } from '../../utils/erros'

/**
 * F6/F11 — detalhe público do evento (só ATIVO; 404 "Evento não encontrado." caso contrário)
 * + inscrição do entusiasta (POST /entusiastas/inscricoes/{idEvento}, corpo só { idCarro? }).
 * vagasDisponiveis nunca é negativa; botão desabilitado quando 0 (a regra final é o 422 do backend).
 */
export function CatalogoDetalhe() {
  const { idEvento = '' } = useParams()
  const navigate = useNavigate()
  const { usuario, autenticado } = useAuth()
  const { toast } = useToast()

  const [evento, setEvento] = useState<CatalogoEventoResponseDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<unknown | null>(null)
  const [inscrevendo, setIncrevendo] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [carros, setCarros] = useState<CarroRespostaDTO[]>([])
  const [idCarroSelecionado, setIdCarroSelecionado] = useState('')

  const podeInscrever = !!usuario && (usuario.tipoUsuario === 'ENTUSIASTA' || usuario.tipoUsuario === 'COLECIONADOR')

  async function carregar() {
    setCarregando(true)
    setErro(null)
    try {
      const resposta = await detalharEventoPublico(idEvento)
      setEvento(resposta.data)
    } catch (e) {
      setErro(e)
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    void carregar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idEvento])

  // Colecionador: carrega a garagem para o select opcional "levar um carro".
  useEffect(() => {
    if (ehColecionador(usuario)) {
      listarCarros(0, 100)
        .then((resposta) => setCarros(resposta.data.conteudo))
        .catch(() => setCarros([]))
    }
  }, [usuario])

  async function aoInscrever() {
    setMensagem(null)
    setIncrevendo(true)
    try {
      const resposta = await inscreverEmEvento(idEvento, {
        idCarro: idCarroSelecionado || null,
      })
      toast('sucesso', `Inscrição confirmada! Status: ${resposta.data.status}`)
      await carregar()
      setMensagem(
        `Inscrição realizada com sucesso.${resposta.data.carroVinculado ? ` Veículo vinculado: ${resposta.data.carroVinculado}.` : ''}`,
      )
    } catch (e) {
      const normalizado = extrairErro(e)
      setMensagem(mensagemDoErro(e))
      // 422 de estado concorrente (lotação/duplicidade): refetch do detalhe.
      if (normalizado.status === 422) await carregar()
    } finally {
      setIncrevendo(false)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando evento..." />

  if (erro) {
    return (
      <div>
        <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>
        <Link to="/eventos" className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar para o catálogo
        </Link>
      </div>
    )
  }

  if (!evento) return null

  const semVagas = evento.vagasDisponiveis <= 0

  return (
    <div>
      <div className="titulo-pagina">
        <h1>{evento.nome}</h1>
        <div className="botoes-inline">
          {evento.inscrito && <span className="badge badge-verde">Você já está inscrito</span>}
          <span className={`badge ${semVagas ? 'badge-vermelho' : 'badge-azul'}`}>{evento.vagasDisponiveis} vaga(s)</span>
        </div>
      </div>

      <p className="card-subtitulo">
        Organizado por <strong>{evento.nomeOrganizador}</strong>
      </p>

      {evento.fotos.length > 0 && (
        <div className="galeria" style={{ marginBottom: 16 }}>
          {evento.fotos.map((url, indice) => (
            <a key={indice} href={url} target="_blank" rel="noreferrer">
              <img src={url} alt={`Foto ${indice + 1} do evento`} />
            </a>
          ))}
        </div>
      )}

      <div className="detalhe-secao">
        <h2>Sobre o evento</h2>
        <p>{evento.descricao}</p>
        <dl className="linha-tempo">
          <dt>Início</dt>
          <dd>{formatarDataHora(evento.dataHoraInicio)}</dd>
          <dt>Fim</dt>
          <dd>{formatarDataHora(evento.dataHoraFim)}</dd>
          <dt>Preço</dt>
          <dd>{formatarMoeda(evento.preco)}</dd>
          <dt>Endereço</dt>
          <dd>
            {evento.rua}, {evento.numero} - {evento.bairro}
            <br />
            {evento.cidade} - {evento.uf} · CEP {formatarCep(evento.cep)}
          </dd>
          <dt>Contato</dt>
          <dd>
            <a href={`mailto:${evento.emailOrganizador}`}>{evento.emailOrganizador}</a>
          </dd>
          <dt>Vagas disponíveis</dt>
          <dd>{evento.vagasDisponiveis}</dd>
        </dl>
        {evento.latitude !== null && evento.longitude !== null && (
          <div className="card-acoes">
            <a
              className="botao botao-secundario"
              style={{ textDecoration: 'none' }}
              href={`https://www.google.com/maps?q=${evento.latitude},${evento.longitude}`}
              target="_blank"
              rel="noreferrer"
            >
              Ver no mapa
            </a>
          </div>
        )}
      </div>

      <div className="detalhe-secao">
        <h2>Inscrição</h2>
        {mensagem && <Alerta tipo={mensagem.startsWith('Inscrição') ? 'sucesso' : 'erro'}>{mensagem}</Alerta>}

        {!autenticado && (
          <p>
            Crie a sua conta para se inscrever: <Link to="/cadastro">Cadastrar-se</Link>
          </p>
        )}

        {autenticado && !podeInscrever && (
          <p className="abreviado">Apenas entusiastas e colecionadores podem se inscrever em eventos.</p>
        )}

        {podeInscrever && evento.inscrito && <p>Você já está inscrito neste evento. 🎉</p>}

        {podeInscrever && !evento.inscrito && (
          <>
            {semVagas && (
              <Alerta tipo="aviso">
                Infelizmente o limite de participantes deste evento já foi atingido (Lotação Esgotada).
              </Alerta>
            )}
            {ehColecionador(usuario) && carros.length > 0 && (
              <CampoSelect
                id="idCarro"
                rotulo="Levar um carro (opcional)"
                placeholder="Sem veículo vinculado"
                opcoes={carros.map((carro) => ({
                  valor: carro.idCarro,
                  rotulo: `${carro.marca} ${carro.modelo} (${carro.placa})`,
                }))}
                value={idCarroSelecionado}
                onChange={(e) => setIdCarroSelecionado(e.target.value)}
              />
            )}
            <div className="form-acoes">
              <Botao onClick={aoInscrever} carregando={inscrevendo} disabled={semVagas}>
                Inscrever-se
              </Botao>
              <Botao variante="secundario" onClick={() => navigate('/eventos')}>
                Voltar
              </Botao>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
