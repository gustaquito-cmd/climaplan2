import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import type { ReactNode } from 'react'
import { Icon, type IconName } from './Icon'

const navigation: { to: string; label: string; icon: IconName }[] = [
  { to: '/dashboard', label: 'Inicio', icon: 'home' },
  { to: '/plans', label: 'Mis planes', icon: 'calendar' },
  { to: '/favorites', label: 'Favoritos', icon: 'heart' },
  { to: '/weather', label: 'Clima', icon: 'cloud' },
]

export function AppLayout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth()
  const items = user?.rol === 'ROLE_ADMIN'
    ? [...navigation, { to: '/admin', label: 'Administración', icon: 'users' as const }]
    : navigation

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="brand" to="/dashboard" aria-label="ClimaPlan inicio">
          <span className="brand-mark">C</span>
          <span>
            <strong>ClimaPlan</strong>
          </span>
        </NavLink>

        <div className="nav-caption">MENÚ</div>
        <nav className="side-nav" aria-label="Navegación principal">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              <span className="nav-icon"><Icon name={item.icon} /></span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="profile-chip">
            <span className="avatar">{user?.nombre?.slice(0, 1).toUpperCase() ?? '?'}</span>
            <span className="profile-copy">
              <strong>{user?.nombre ?? 'Cuenta'}</strong>
              <small>{user?.email ?? 'ClimaPlan'}</small>
            </span>
          </div>
          <button className="logout-button" onClick={logout} type="button">
            <Icon name="logout" /> Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <span className="topbar-date">ClimaPlan</span>
          <span className="topbar-user">{user?.email}</span>
        </header>
        <div className="page-content">{children}</div>
      </main>
    </div>
  )
}
