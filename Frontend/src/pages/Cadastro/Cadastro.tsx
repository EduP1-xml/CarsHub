import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { TipoPrestadorServico, TipoUsuario } from '../../types/api'
import { useToast } from '../../context/ToastContext'
import { Etapa1 } from './Etapa1'
import { Etapa2 } from './Etapa2'
import { Etapa3 } from './Etapa3'

const ROTULOS = ['Dados da conta', 'Dados do perfil', 'Verificação do e-mail']

/**
 * F1/F2 — cadastro em 3 etapas (mandatório):
 *  1) usuário base (e-mail + senha) — SEM COLECIONADOR no select (backend rejeita com 422);
 *  2) dados por tipo (CPF | CNPJ | CNPJ+serviços | CNPJ+placa);
 *  3) código de 6 dígitos (10 min) com reenvio (cooldown 60 s).
 */
export function Cadastro() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const [etapa, setEtapa] = useState(1)
  const [email, setEmail] = useState('')
  const [tipoUsuario, setTipoUsuario] = useState<TipoUsuario | null>(null)
  const [tipoPrestador, setTipoPrestador] = useState<TipoPrestadorServico | null>(null)

  function aoConcluirEtapa1(novoEmail: string, tipo: TipoUsuario, prestador: TipoPrestadorServico | null) {
    setEmail(novoEmail)
    setTipoUsuario(tipo)
    setTipoPrestador(prestador)
    setEtapa(2)
  }

  function aoConcluirEtapa3() {
    toast('sucesso', 'E-mail verificado com sucesso. Você já pode fazer login no CarsHub.')
    navigate('/login')
  }

  return (
    <div className="pagina-publica">
      <h1>Criar conta</h1>
      <p className="subtitulo">Cadastro em 3 etapas</p>

      <div className="etapas" aria-label="Etapas do cadastro">
        {ROTULOS.map((rotulo, indice) => {
          const numero = indice + 1
          const classe = numero === etapa ? 'etapa etapa-ativa' : numero < etapa ? 'etapa etapa-concluida' : 'etapa'
          return (
            <div key={rotulo} className={classe}>
              {numero}. {rotulo}
            </div>
          )
        })}
      </div>

      {etapa === 1 && <Etapa1 aoConcluir={aoConcluirEtapa1} />}
      {etapa === 2 && email && tipoUsuario && (
        <Etapa2 email={email} tipoUsuario={tipoUsuario} tipoPrestador={tipoPrestador} aoConcluir={() => setEtapa(3)} />
      )}
      {etapa === 3 && email && <Etapa3 email={email} aoConcluir={aoConcluirEtapa3} />}

      <p className="links-rodape">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </div>
  )
}
