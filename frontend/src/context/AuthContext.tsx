import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { usersApi } from '../api/users'
import {
  clearAccessToken,
  getAccessToken,
  storeAccessToken,
} from '../api/session'
import type { AuthResponse, Usuario } from '../types'

interface AuthContextValue {
  token: string | null
  user: Usuario | null
  loading: boolean
  profileError: string | null
  setAuthenticated: (response: AuthResponse) => void
  reloadProfile: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [token, setToken] = useState<string | null>(() =>
    getAccessToken(),
  )
  const [user, setUser] = useState<Usuario | null>(null)
  const [loading, setLoading] = useState(Boolean(token))
  const [profileError, setProfileError] = useState<string | null>(null)

  const reloadProfile = useCallback(async () => {
    if (!token) {
      setUser(null)
      setLoading(false)
      return
    }

    setLoading(true)
    setProfileError(null)
    try {
      setUser(await usersApi.me())
    } catch (error) {
      setUser(null)
      setProfileError(getApiErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    void reloadProfile()
  }, [reloadProfile])

  useEffect(() => {
    const handleUnauthorized = () => {
      setToken(null)
      setUser(null)
      setLoading(false)
      setProfileError(null)
      if (location.pathname !== '/login' && location.pathname !== '/register') {
        navigate('/login', {
          replace: true,
          state: { message: 'Tu sesión expiró. Inicia sesión de nuevo.' },
        })
      }
    }

    window.addEventListener('climaplan:unauthorized', handleUnauthorized)
    return () =>
      window.removeEventListener('climaplan:unauthorized', handleUnauthorized)
  }, [location.pathname, navigate])

  const setAuthenticated = useCallback((response: AuthResponse) => {
    storeAccessToken(response.accessToken)
    setUser(response.usuario)
    setProfileError(null)
    setLoading(true)
    setToken(response.accessToken)
  }, [])

  const logout = useCallback(() => {
    clearAccessToken()
    setToken(null)
    setUser(null)
    setProfileError(null)
    setLoading(false)
    navigate('/login', { replace: true })
  }, [navigate])

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      profileError,
      setAuthenticated,
      reloadProfile,
      logout,
    }),
    [token, user, loading, profileError, setAuthenticated, reloadProfile, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider')
  }
  return context
}
