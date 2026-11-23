import { Link } from 'react-router-dom'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'

const shortcuts = [
  {
    to: '/plans',
    icon: '✈',
    title: 'Mis planes',
    description: 'Organiza y consulta tus próximas actividades.',
    action: 'Ver planes',
    tone: 'sky',
  },
  {
    to: '/weather',
    icon: '☼',
    title: 'Consulta el clima',
    description: 'Revisa las condiciones actuales de cualquier ciudad.',
    action: 'Consultar clima',
    tone: 'sun',
  },
  {
    to: '/favorites',
    icon: '♡',
    title: 'Destinos favoritos',
    description: 'Guarda lugares que quieres conocer.',
    action: 'Ver favoritos',
    tone: 'mint',
  },
]

export function DashboardPage() {
  const { user, loading, profileError, reloadProfile } = useAuth()

  if (loading) return <LoadingState label="Cargando tu perfil..." />
  if (profileError) {
    return (
      <section>
        <ErrorMessage message={profileError} />
        <button className="button button-secondary" onClick={() => void reloadProfile()}>
          Reintentar
        </button>
      </section>
    )
  }

  return (
    <section>
      <div className="welcome-panel">
        <div>
          <span className="eyebrow eyebrow-light">TU TABLERO PERSONAL</span>
          <h1>Hola, {user?.nombre?.split(' ')[0] ?? 'viajero'} <span aria-hidden="true">✦</span></h1>
          <p>¿Qué lugar te gustaría descubrir hoy?</p>
        </div>
        <div className="welcome-weather" aria-hidden="true">
          <span>☀</span>
          <small>un buen día<br />para planear</small>
        </div>
      </div>

      <div className="section-heading dashboard-heading">
        <div>
          <span className="eyebrow">TU CUENTA</span>
          <h2>Todo listo para tu próxima aventura</h2>
        </div>
      </div>

      <div className="account-card">
        <div className="account-avatar">{user?.nombre?.slice(0, 1).toUpperCase() ?? '?'}</div>
        <div className="account-details">
          <strong>{user?.nombre}</strong>
          <span>{user?.email}</span>
        </div>
        <span className="role-badge">{user?.rol}</span>
      </div>

      <div className="section-heading">
        <div>
          <span className="eyebrow">ACCESOS RÁPIDOS</span>
          <h2>¿A dónde vamos?</h2>
        </div>
      </div>

      <div className="shortcut-grid">
        {shortcuts.map((shortcut) => (
          <Link className="shortcut-card" to={shortcut.to} key={shortcut.to}>
            <span className={`shortcut-icon ${shortcut.tone}`} aria-hidden="true">{shortcut.icon}</span>
            <h3>{shortcut.title}</h3>
            <p>{shortcut.description}</p>
            <span className="shortcut-action">{shortcut.action} <span aria-hidden="true">→</span></span>
          </Link>
        ))}
      </div>
    </section>
  )
}
