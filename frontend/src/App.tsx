import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { LoadingState } from './components/Feedback'
import { DashboardPage } from './pages/DashboardPage'
import { FavoritesPage } from './pages/FavoritesPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PlansPage } from './pages/PlansPage'
import { RegisterPage } from './pages/RegisterPage'
import { WeatherPage } from './pages/WeatherPage'

function HomeRedirect() {
  const { token, loading } = useAuth()
  if (loading) return <LoadingState label="Preparando ClimaPlan..." />
  return <Navigate to={token ? '/dashboard' : '/login'} replace />
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/plans" element={<PlansPage />} />
        <Route path="/favorites" element={<FavoritesPage />} />
        <Route path="/weather" element={<WeatherPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
