import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { AppLayout } from './AppLayout'

export function AdminRoute() {
  const { user } = useAuth()

  if (user?.rol !== 'ROLE_ADMIN') {
    return (
      <Navigate
        to="/dashboard"
        replace
        state={{ adminAccessDenied: true }}
      />
    )
  }

  return <Outlet />
}

export function ProtectedRoute() {
  const { token, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <main className="center-state">
        <span className="loader" aria-hidden="true" />
        <p>Comprobando tu sesión...</p>
      </main>
    )
  }

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  )
}
