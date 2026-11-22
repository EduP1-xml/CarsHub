import { useState, type FormEvent } from 'react'
import { Alerta } from '../../components/Alerta'
import { Botao } from '../../components/Botao'
import { CampoSelect, CampoTexto } from '../../components/FormField'
import { cadastrarUsuario } from '../../api/auth'
import { useForm } from '../../hooks/useForm'
import type { TipoPrestadorServico, TipoUsuario } from '../../types/api'
import { extrairErro, mensagemDoErro } from '../../utils/erros'
import { dataBrasileiraParaIso, mascaraData, REGRAS_SENHA_TEXTO, validarSenhaForte } from '../../utils/validacoes'

interface FormEtapa1 {
  nome: string
  login: string
  senha: string
  dataNascimento: string
  telefone: string
  tipoUsuario: TipoUsuario | ''
  tipoPrestadorServico: TipoPrestadorServico | ''
}

// F1: SEM COLECIONADOR no cadastro (o título nasce com o 1º carro).
const OPCOES_TIPO: { valor: TipoUsuario; rotulo: string }[] = [
  { valor: 'ENTUSIASTA', rotulo: 'Entusiasta' },
  { valor: 'ORGANIZADOR_EVENTO', rotulo: 'Organizador de eventos' },
  { valor: 'PRESTADOR_SERVICO', rotulo: 'Prestador de serviços' },
]

const OPCOES_SUBTIPO: { valor: TipoPrestadorServico; rotulo: string }[] = [
  { valor: 'MECANICO', rotulo: 'Mecânico' },
  { valor: 'GUINCHO', rotulo: 'Guincho' },
]

interface Etapa1Props {
  aoConcluir: (email: string, tipoUsuario: TipoUsuario, tipoPrestador: TipoPrestadorServico | null) => void
}

export function Etapa1({ aoConcluir }: Etapa1Props) {
  const { valores, setValor, errosCampos, setErrosCampos } = useForm<FormEtapa1>({
    nome: '',
    login: '',
    senha: '',
    dataNascimento: '',
    telefone: '',
    tipoUsuario: '',
    tipoPrestadorServico: '',
  })
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  const ehPrestador = valores.tipoUsuario === 'PRESTADOR_SERVICO'

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setMensagem(null)
    setErrosCampos({})

    const politica = validarSenhaForte(valores.senha)
    if (!politica.valida) {
      setMensagem(`Senha fraca: ${politica.problemas.join(', ')}.`)
      return
    }

    // Data dd/mm/aaaa → ISO (LocalDate do backend). Vazia → null (400 "obrigatória").
    const dataIso = valores.dataNascimento.trim() === '' ? null : dataBrasileiraParaIso(valores.dataNascimento)
    if (valores.dataNascimento.trim() !== '' && dataIso === null) {
      setErrosCampos({ dataNascimento: 'Data de nascimento inválida. Use o formato dd/mm/aaaa.' })
      return
    }

    setEnviando(true)
    try {
      await cadastrarUsuario({
        nome: valores.nome.trim(),
        login: valores.login.trim(),
        senha: valores.senha,
        // LocalDate: null gera o 400 "A data de nascimento é obrigatória" (mapa por campo).
        dataNascimento: (dataIso ?? null) as unknown as string,
        telefone: valores.telefone.trim(),
        tipoUsuario: valores.tipoUsuario as TipoUsuario,
        tipoPrestadorServico: ehPrestador ? (valores.tipoPrestadorServico as TipoPrestadorServico) : null,
      })
      aoConcluir(valores.login.trim(), valores.tipoUsuario as TipoUsuario, ehPrestador ? (valores.tipoPrestadorServico as TipoPrestadorServico) : null)
    } catch (erro) {
      const normalizado = extrairErro(erro)
      if (normalizado.campos) {
        setErrosCampos(normalizado.campos)
      } else {
        setMensagem(mensagemDoErro(erro))
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="form-secao">
      {mensagem && <Alerta tipo="erro">{mensagem}</Alerta>}
      <form className="form-grid" onSubmit={aoEnviar}>
        <div className="campo-largo">
          <CampoTexto
            id="nome"
            rotulo="Nome"
            obrigatorio
            maxLength={100}
            value={valores.nome}
            erro={errosCampos.nome}
            onChange={(e) => setValor('nome', e.target.value)}
          />
        </div>
        <CampoTexto
          id="login"
          rotulo="E-mail (login)"
          type="email"
          obrigatorio
          value={valores.login}
          erro={errosCampos.login}
          onChange={(e) => setValor('login', e.target.value)}
        />
        <CampoTexto
          id="dataNascimento"
          rotulo="Data de nascimento"
          obrigatorio
          type="text"
          inputMode="numeric"
          maxLength={10}
          placeholder="dd/mm/aaaa"
          autoComplete="bday"
          value={valores.dataNascimento}
          erro={errosCampos.dataNascimento}
          onChange={(e) => setValor('dataNascimento', mascaraData(e.target.value, valores.dataNascimento))}
        />
        <div className="campo-largo">
          <CampoTexto
            id="senha"
            rotulo="Senha"
            type="password"
            olhoSenha
            obrigatorio
            autoComplete="new-password"
            value={valores.senha}
            erro={errosCampos.senha}
            ajuda={REGRAS_SENHA_TEXTO}
            onChange={(e) => setValor('senha', e.target.value)}
          />
        </div>
        <CampoTexto
          id="telefone"
          rotulo="Telefone"
          obrigatorio
          maxLength={17}
          value={valores.telefone}
          erro={errosCampos.telefone}
          onChange={(e) => setValor('telefone', e.target.value)}
          placeholder="(11) 99999-9999"
        />
        <CampoSelect
          id="tipoUsuario"
          rotulo="Tipo de usuário"
          obrigatorio
          placeholder="Selecione..."
          opcoes={OPCOES_TIPO}
          value={valores.tipoUsuario}
          erro={errosCampos.tipoUsuario}
          onChange={(e) => {
            setValor('tipoUsuario', e.target.value as TipoUsuario | '')
            setValor('tipoPrestadorServico', '')
          }}
        />
        {ehPrestador && (
          <CampoSelect
            id="tipoPrestadorServico"
            rotulo="Tipo de prestador"
            obrigatorio
            placeholder="Selecione..."
            opcoes={OPCOES_SUBTIPO}
            value={valores.tipoPrestadorServico}
            erro={errosCampos.tipoPrestadorServico}
            onChange={(e) => setValor('tipoPrestadorServico', e.target.value as TipoPrestadorServico | '')}
          />
        )}
        <div className="campo-largo form-acoes">
          <Botao type="submit" carregando={enviando}>
            Continuar
          </Botao>
        </div>
      </form>
    </div>
  )
}
