import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { AppLayout } from './AppLayout'

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
