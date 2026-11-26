import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CarrosselFotos } from '../../components/CarrosselFotos'
import { LoadingPagina } from '../../components/Loading'
import { Modal } from '../../components/Modal'
import { buscarCarro, listarCarros, removerCarro } from '../../api/carros'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import type { CarroRespostaDTO } from '../../types/api'
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
} from '../../utils/enums'
import { mensagemDoErro } from '../../utils/erros'

function SimNao(valor: boolean): string {
  return valor ? 'Sim' : 'Não'
}

/** F13 — detalhe COMPLETO do carro (todos os campos + fotosCarro — URLs vindas do backend). */
export function CarroDetalhe() {
  const { idCarro = '' } = useParams()
  const navigate = useNavigate()
  const { atualizarSessaoLocal } = useAuth()
  const { toast } = useToast()
  const [carro, setCarro] = useState<CarroRespostaDTO | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<unknown | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const [excluindo, setExcluindo] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  useEffect(() => {
    let cancelado = false
    setCarregando(true)
    setErro(null)
    buscarCarro(idCarro)
      .then((resposta) => {
        if (!cancelado) setCarro(resposta.data)
      })
      .catch((e) => {
        if (!cancelado) setErro(e)
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })
    return () => {
      cancelado = true
    }
  }, [idCarro])

  async function aoExcluir() {
    setExcluindo(true)
    setMensagem(null)
    try {
      await removerCarro(idCarro)
      // Virada do título: se for o último carro, volta a entusiasta (espelho da UI).
      const garagem = await listarCarros(0, 5)
      if (garagem.data.totalElementos === 0) {
        atualizarSessaoLocal({ tipoUsuario: 'ENTUSIASTA' })
        toast('info', 'Sua garagem ficou vazia: você voltou a ser Entusiasta.')
      }
      toast('sucesso', 'Carro removido da garagem.')
      navigate('/garagem')
    } catch (e) {
      setMensagem(mensagemDoErro(e))
      setConfirmando(false)
    } finally {
      setExcluindo(false)
    }
  }

  if (carregando) return <LoadingPagina rotulo="Carregando carro..." />

  if (erro || !carro) {
    return (
      <div>
        <Alerta tipo="erro">{mensagemDoErro(erro)}</Alerta>
        <Link to="/garagem" className="botao botao-secundario" style={{ textDecoration: 'none' }}>
          ← Voltar para a garagem
        </Link>
      </div>
    )
  }

  const linhas: [string, string][] = [
    ['Marca / Modelo', `${carro.marca} ${carro.modelo}`],
    ['Placa', carro.placa],
    ['Proprietário', carro.nomeProprietario],
    ['Carroceria', LABEL_CARROCERIA[carro.carroceria]],
    ['Ano', String(carro.ano)],
    ['Cor', carro.cor],
    ['Cilindros', String(carro.quantidadeCilindros)],
    ['Motor', LABEL_MOTOR[carro.motor]],
    ['Litragem', String(carro.litragemMotor)],
    ['Injeção', LABEL_INJECAO[carro.injecao]],
    ['Alimentação', LABEL_ALIMENTACAO[carro.alimentacao]],
    ['Combustível', LABEL_COMBUSTIVEL[carro.combustivel]],
    ['Câmbio', LABEL_CAMBIO[carro.cambio]],
    ['Tração', LABEL_TRACAO[carro.tracao]],
    ['Suspensão', LABEL_SUSPENSAO[carro.suspensao]],
    ['Freio', LABEL_FREIO[carro.freio]],
    ['Direção', LABEL_DIRECAO[carro.direcao]],
    ['Roda', LABEL_RODA[carro.roda]],
    ['Aro', String(carro.aro)],
    ['Portas', String(carro.numeroPortas)],
    ['Assentos', String(carro.numeroAssentos)],
    ['Revestimento interno', LABEL_REVESTIMENTO[carro.materialRevestimentoInterno]],
    ['Vidros elétricos', SimNao(carro.vidrosEletricos)],
    ['Som', SimNao(carro.som)],
    ['Ar-condicionado', SimNao(carro.arCondicionado)],
    ['Teto solar', SimNao(carro.tetoSolar)],
  ]

  return (
    <div>
      <div className="titulo-pagina">
        <h1>
          {carro.marca} {carro.modelo}
        </h1>
        <div className="botoes-inline">
          <Link to={`/garagem/${carro.idCarro}/editar`} className="botao botao-secundario" style={{ textDecoration: 'none' }}>
            Editar
          </Link>
          <Link
            to={`/garagem/${carro.idCarro}/historico`}
            className="botao botao-secundario"
            style={{ textDecoration: 'none' }}
          >
            Histórico de manutenções
          </Link>
          <Botao variante="perigo" onClick={() => setConfirmando(true)}>
            Excluir
          </Botao>
        </div>
      </div>

      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}

      {/* Dados do carro à esquerda e fotos (carrossel) à direita, no mesmo bloco. */}
      <div className="detalhe-secao detalhe-carro">
        <dl className="linha-tempo detalhe-carro-dados">
          {linhas.map(([rotulo, valor]) => (
            <div key={rotulo} style={{ display: 'contents' }}>
              <dt>{rotulo}</dt>
              <dd>{valor}</dd>
            </div>
          ))}
        </dl>

        {carro.fotosCarro.length > 0 && (
          <div className="detalhe-carro-fotos">
            <CarrosselFotos
              fotos={carro.fotosCarro}
              alt={`${carro.marca} ${carro.modelo}`}
              abrirEmNovaAba
            />
          </div>
        )}
      </div>

      <Modal
        aberto={confirmando}
        titulo="Excluir carro"
        confirmarRotulo="Excluir"
        varianteConfirmar="perigo"
        confirmarCarregando={excluindo}
        aoConfirmar={aoExcluir}
        aoFechar={() => setConfirmando(false)}
      >
        <p>
          Excluir <strong>{carro.marca} {carro.modelo}</strong> ({carro.placa})? Se for o seu último carro, você
          voltará a ser Entusiasta.
        </p>
      </Modal>
    </div>
  )
}
