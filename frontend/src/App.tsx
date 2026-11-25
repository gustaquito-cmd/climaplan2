import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { AdminRoute, ProtectedRoute } from './components/ProtectedRoute'
import { LoadingState } from './components/Feedback'
import { DashboardPage } from './pages/DashboardPage'
import { AdminPage } from './pages/AdminPage'
import { FavoritesPage } from './pages/FavoritesPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PlansPage } from './pages/PlansPage'
import { PlanChatPage } from './pages/PlanChatPage'
import { RegisterPage } from './pages/RegisterPage'
import { SharedPlanPage } from './pages/SharedPlanPage'
import { WeatherPage } from './pages/WeatherPage'
import { ToastProvider } from './context/ToastContext'

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
      <Route path="/shared/:token" element={<SharedPlanPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/plans" element={<PlansPage />} />
        <Route path="/plans/:planId/chat" element={<PlanChatPage />} />
        <Route path="/favorites" element={<FavoritesPage />} />
        <Route path="/weather" element={<WeatherPage />} />
        <Route element={<AdminRoute />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <AppRoutes />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
