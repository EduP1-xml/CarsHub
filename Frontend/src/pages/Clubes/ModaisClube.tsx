// Modais compartilhados entre a lista e o detalhe do clube.
import { useState } from 'react'
import { desvincularCarro, entrarNoClube, vincularCarro } from '../../api/clubes'
import { Alerta } from '../../components/Alerta'
import { Modal } from '../../components/Modal'
import { SeletorCarrosCompativeis } from '../../components/SeletorCarrosCompativeis'
import { ehColecionador, useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import type { ClubeRespostaDTO } from '../../types/api'
import { mensagemDoErro } from '../../utils/erros'

interface ModalEntrarClubeProps {
  clube: ClubeRespostaDTO | null
  aoFechar: () => void
  aoEntrar: (clube: ClubeRespostaDTO) => void
}

/** "Entrar": entrada livre; o colecionador pode já escolher os carros compatíveis a vincular. */
export function ModalEntrarClube({ clube, aoFechar, aoEntrar }: ModalEntrarClubeProps) {
  const { usuario } = useAuth()
  const { toast } = useToast()
  const [selecionados, setSelecionados] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  if (!clube) return null
  const colecionador = ehColecionador(usuario)

  function fechar() {
    setSelecionados([])
    setMensagem(null)
    aoFechar()
  }

  async function confirmar() {
    if (!clube) return
    setEnviando(true)
    setMensagem(null)
    try {
      const resposta = await entrarNoClube(clube.idClube, selecionados)
      toast('sucesso', `Você entrou no clube ${clube.nome}!`)
      setSelecionados([])
      aoEntrar(resposta.data)
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      aberto
      titulo={`Entrar no clube ${clube.nome}`}
      confirmarRotulo="Entrar"
      confirmarCarregando={enviando}
      aoConfirmar={() => void confirmar()}
      aoFechar={fechar}
    >
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      {colecionador ? (
        <>
          <p>Escolha os carros da sua garagem que você quer vincular ao clube (opcional):</p>
          <SeletorCarrosCompativeis idClube={clube.idClube} selecionados={selecionados} aoMudar={setSelecionados} />
        </>
      ) : (
        <p>A entrada é livre. Quando você tiver carros na garagem, poderá vinculá-los ao clube.</p>
      )}
    </Modal>
  )
}

interface ModalGerenciarCarrosProps {
  idClube: string
  aberto: boolean
  aoFechar: () => void
  aoSalvar: () => void
}

/** "Gerenciar meus carros": marca/desmarca os carros compatíveis vinculados ao clube. */
export function ModalGerenciarCarros({ idClube, aberto, aoFechar, aoSalvar }: ModalGerenciarCarrosProps) {
  const { toast } = useToast()
  const [iniciais, setIniciais] = useState<string[]>([])
  const [selecionados, setSelecionados] = useState<string[]>([])
  const [enviando, setEnviando] = useState(false)
  const [mensagem, setMensagem] = useState<string | null>(null)

  if (!aberto) return null

  async function salvar() {
    setEnviando(true)
    setMensagem(null)
    try {
      const vincular = selecionados.filter((id) => !iniciais.includes(id))
      const desvincular = iniciais.filter((id) => !selecionados.includes(id))
      for (const idCarro of vincular) await vincularCarro(idClube, idCarro)
      for (const idCarro of desvincular) await desvincularCarro(idClube, idCarro)
      toast('sucesso', 'Carros do clube atualizados.')
      aoSalvar()
    } catch (erro) {
      setMensagem(mensagemDoErro(erro))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      aberto
      titulo="Meus carros neste clube"
      confirmarRotulo="Salvar"
      confirmarCarregando={enviando}
      aoConfirmar={() => void salvar()}
      aoFechar={aoFechar}
    >
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <p>Só aparecem os carros da sua garagem que atendem ao gosto do clube.</p>
      <SeletorCarrosCompativeis
        idClube={idClube}
        selecionados={selecionados}
        aoMudar={setSelecionados}
        aoCarregar={(carros) => {
          const vinculados = carros.filter((c) => c.vinculado).map((c) => c.idCarro)
          setIniciais(vinculados)
          setSelecionados(vinculados)
        }}
      />
    </Modal>
  )
}
