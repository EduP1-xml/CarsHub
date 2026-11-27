import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import type { Ator } from '../../api/perfis'
import { AbaDados } from './AbaDados'
import { AbaFoto } from './AbaFoto'
import { AbaLocalizacao } from './AbaLocalizacao'
import { AbaSeguranca } from './AbaSeguranca'

/** Ator (prefixo das rotas) derivado do perfil logado. */
export function atorDoUsuario(
  tipoUsuario: string,
  tipoPrestadorServico: string | null,
): Ator {
  if (tipoUsuario === 'ORGANIZADOR_EVENTO') return 'organizadores'
  if (tipoUsuario === 'PRESTADOR_SERVICO') {
    return tipoPrestadorServico === 'MECANICO' ? 'mecanicos' : 'guinchos'
  }
  return 'entusiastas'
}

type Aba = 'dados' | 'localizacao' | 'foto' | 'seguranca'

/**
 * Perfil dos 4 atores (F7/F9/F10 entusiasta · F28 organizador · F35 prestadores).
 * GET /{ator}/perfil pré-carrega PerfilUsuarioDTO (inclui latitude/longitude só para
 * leitura — a escrita fica no PUT /localizacao). O organizador NÃO tem rota de localização.
 */
export function Perfil() {
  const { usuario } = useAuth()
  const [aba, setAba] = useState<Aba>('dados')

  if (!usuario) return null

  const ator = atorDoUsuario(usuario.tipoUsuario, usuario.tipoPrestadorServico)
  const temLocalizacao = usuario.tipoUsuario !== 'ORGANIZADOR_EVENTO'

  return (
    <div>
      <div className="titulo-pagina">
        <h1>Meu perfil</h1>
      </div>

      <div className="tabs">
        <button type="button" className={`tab${aba === 'dados' ? ' tab-ativa' : ''}`} onClick={() => setAba('dados')}>
          Dados
        </button>
        {temLocalizacao && (
          <button
            type="button"
            className={`tab${aba === 'localizacao' ? ' tab-ativa' : ''}`}
            onClick={() => setAba('localizacao')}
          >
            Localização
          </button>
        )}
        <button type="button" className={`tab${aba === 'foto' ? ' tab-ativa' : ''}`} onClick={() => setAba('foto')}>
          Foto de perfil
        </button>
        <button
          type="button"
          className={`tab${aba === 'seguranca' ? ' tab-ativa' : ''}`}
          onClick={() => setAba('seguranca')}
        >
          Segurança
        </button>
      </div>

      {aba === 'dados' && <AbaDados ator={ator} />}
      {aba === 'localizacao' && temLocalizacao && <AbaLocalizacao ator={ator} />}
      {aba === 'foto' && <AbaFoto ator={ator} />}
      {aba === 'seguranca' && <AbaSeguranca ator={ator} />}
    </div>
  )
}
