import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../context/AuthContext'
import { ErrorMessage } from '../components/Feedback'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { setAuthenticated } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(
    typeof location.state === 'object' &&
      location.state !== null &&
      'message' in location.state &&
      typeof location.state.message === 'string'
      ? location.state.message
      : '',
  )
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const response = await authApi.login({ email, password })
      setAuthenticated(response)
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-visual">
        <Link className="brand brand-light" to="/login">
          <span className="brand-mark">C</span>
          <span><strong>ClimaPlan</strong><small>viaja con buen tiempo</small></span>
        </Link>
        <div className="auth-visual-copy">
          <span className="eyebrow eyebrow-light">PLANEA SIN IMPROVISAR</span>
          <h1>Tu próxima aventura, con el clima de tu lado.</h1>
          <p>Organiza tus planes y descubre qué te espera en cada destino.</p>
        </div>
        <div className="sun-illustration" aria-hidden="true">
          <span className="sun-core" />
          <span className="sun-cloud cloud-one" />
          <span className="sun-cloud cloud-two" />
        </div>
        <span className="auth-visual-note">Menos pendientes. Más lugares por descubrir.</span>
      </section>

      <section className="auth-form-side">
        <div className="auth-card">
          <span className="eyebrow">BIENVENIDO DE NUEVO</span>
          <h2>Inicia sesión</h2>
          <p className="muted">Ingresa tus datos para continuar con tus planes.</p>

          {notice && <div className="feedback feedback-info" role="status">{notice}</div>}
          {error && <ErrorMessage message={error} />}

          <form className="form-stack" onSubmit={handleSubmit} aria-label="Iniciar sesión">
            <label>
              Correo electrónico
              <input
                autoComplete="email"
                name="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="tu@correo.com"
                required
              />
            </label>
            <label>
              Contraseña
              <input
                autoComplete="current-password"
                name="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Tu contraseña"
                required
              />
            </label>
            <button className="button button-primary button-wide" disabled={submitting}>
              {submitting ? 'Entrando...' : 'Entrar a ClimaPlan'}
              {!submitting && <span aria-hidden="true">→</span>}
            </button>
          </form>

          <p className="auth-switch">
            ¿Aún no tienes cuenta? <Link to="/register">Crear cuenta</Link>
          </p>
        </div>
        <span className="auth-footer">ClimaPlan · planea mejor, viaja más</span>
      </section>
    </main>
  )
}
