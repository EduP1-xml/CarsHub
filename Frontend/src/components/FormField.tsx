import { useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

interface BaseProps {
  rotulo?: string
  erro?: string
  ajuda?: string
  obrigatorio?: boolean
  children?: ReactNode
}

function erroOuAjuda(erro?: string, ajuda?: string) {
  if (erro) return <span className="campo-erro">{erro}</span>
  if (ajuda) return <span className="campo-ajuda">{ajuda}</span>
  return null
}

interface InputProps extends BaseProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'children'> {
  /** type="password" + olhoSenha: exibe o botão de mostrar/ocultar a senha. */
  olhoSenha?: boolean
}

export function CampoTexto({ rotulo, erro, ajuda, obrigatorio, id, type, olhoSenha, ...resto }: InputProps) {
  const [senhaVisivel, setSenhaVisivel] = useState(false)
  const ehSenhaComOlho = type === 'password' && olhoSenha
  const tipoEfetivo = ehSenhaComOlho && senhaVisivel ? 'text' : type

  return (
    <label className="campo" htmlFor={id}>
      {rotulo && (
        <span className="campo-rotulo">
          {rotulo}
          {obrigatorio && <span className="campo-obrigatorio"> *</span>}
        </span>
      )}
      <span className={`campo-senha${ehSenhaComOlho ? ' campo-senha-com-olho' : ''}`}>
        <input id={id} type={tipoEfetivo} className={`campo-input${erro ? ' campo-input-invalido' : ''}`} {...resto} />
        {ehSenhaComOlho && (
          <button
            type="button"
            className="campo-senha-olho"
            aria-label={senhaVisivel ? 'Ocultar senha' : 'Exibir senha'}
            title={senhaVisivel ? 'Ocultar senha' : 'Exibir senha'}
            onClick={() => setSenhaVisivel((v) => !v)}
          >
            {senhaVisivel ? '🙈' : '👁️'}
          </button>
        )}
      </span>
      {erroOuAjuda(erro, ajuda)}
    </label>
  )
}

interface SelectProps extends BaseProps, Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  opcoes: { valor: string; rotulo: string }[]
  placeholder?: string
}

export function CampoSelect({
  rotulo,
  erro,
  ajuda,
  obrigatorio,
  id,
  opcoes,
  placeholder,
  value,
  ...resto
}: SelectProps) {
  return (
    <label className="campo" htmlFor={id}>
      {rotulo && (
        <span className="campo-rotulo">
          {rotulo}
          {obrigatorio && <span className="campo-obrigatorio"> *</span>}
        </span>
      )}
      <select
        id={id}
        className={`campo-input${erro ? ' campo-input-invalido' : ''}`}
        value={value ?? ''}
        {...resto}
      >
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {opcoes.map((opcao) => (
          <option key={opcao.valor} value={opcao.valor}>
            {opcao.rotulo}
          </option>
        ))}
      </select>
      {erroOuAjuda(erro, ajuda)}
    </label>
  )
}

interface AreaProps extends BaseProps, Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'children'> {}

export function CampoArea({ rotulo, erro, ajuda, obrigatorio, id, ...resto }: AreaProps) {
  return (
    <label className="campo" htmlFor={id}>
      {rotulo && (
        <span className="campo-rotulo">
          {rotulo}
          {obrigatorio && <span className="campo-obrigatorio"> *</span>}
        </span>
      )}
      <textarea id={id} className={`campo-input${erro ? ' campo-input-invalido' : ''}`} {...resto} />
      {erroOuAjuda(erro, ajuda)}
    </label>
  )
}
