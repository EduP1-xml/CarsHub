import { Outlet } from 'react-router-dom'
import { NavBar } from './NavBar'

export function Layout() {
  return (
    <div className="app">
      <NavBar />
      <main className="conteudo">
        <Outlet />
      </main>
      <footer className="rodape">CarsHub — frontend React (Vite) · API em http://localhost:8080/api/carshub</footer>
    </div>
  )
}
