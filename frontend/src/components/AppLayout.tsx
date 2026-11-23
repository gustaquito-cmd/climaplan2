import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { ReactNode } from 'react'

const navigation = [
  { to: '/dashboard', label: 'Inicio', icon: '⌂' },
  { to: '/plans', label: 'Mis planes', icon: '✈' },
  { to: '/favorites', label: 'Favoritos', icon: '♡' },
  { to: '/weather', label: 'Clima', icon: '☼' },
]

export function AppLayout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="brand" to="/dashboard" aria-label="ClimaPlan inicio">
          <span className="brand-mark">C</span>
          <span>
            <strong>ClimaPlan</strong>
            <small>viaja con buen tiempo</small>
          </span>
        </NavLink>

        <div className="nav-caption">MENÚ</div>
        <nav className="side-nav" aria-label="Navegación principal">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              <span className="nav-icon" aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="profile-chip">
            <span className="avatar">{user?.nombre?.slice(0, 1).toUpperCase() ?? '?'}</span>
            <span className="profile-copy">
              <strong>{user?.nombre ?? 'Cuenta'}</strong>
              <small>{user?.rol?.replace('ROLE_', '') ?? 'ClimaPlan'}</small>
            </span>
          </div>
          <button className="logout-button" onClick={logout} type="button">
            <span aria-hidden="true">↗</span> Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <span className="topbar-date">TU ESPACIO DE VIAJE</span>
          <span className="topbar-user">{user?.email}</span>
        </header>
        <div className="page-content">{children}</div>
      </main>
    </div>
  )
}
