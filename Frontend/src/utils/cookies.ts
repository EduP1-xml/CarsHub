// Utilitário de cookies — usado para ler o cookie XSRF-TOKEN (double-submit CSRF).
export function getCookie(nome: string): string | null {
  const prefixo = `${nome}=`
  const partes = document.cookie.split(';')
  for (const parte of partes) {
    const pedaco = parte.trim()
    if (pedaco.startsWith(prefixo)) {
      return decodeURIComponent(pedaco.slice(prefixo.length))
    }
  }
  return null
}
