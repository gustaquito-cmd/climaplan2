import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="not-found-page">
      <span className="not-found-mark" aria-hidden="true">404</span>
      <span className="eyebrow">CAMINO NO ENCONTRADO</span>
      <h1>Esta ruta no aparece en el mapa.</h1>
      <p>Vuelve al inicio y encuentra tu siguiente destino.</p>
      <Link className="button button-primary" to="/dashboard">Volver a ClimaPlan</Link>
    </main>
  )
}
