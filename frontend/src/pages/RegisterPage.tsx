import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { useAuth } from '../context/AuthContext'
import { ErrorMessage } from '../components/Feedback'

export function RegisterPage() {
  const navigate = useNavigate()
  const { setAuthenticated } = useAuth()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (password !== confirmation) {
      setError('Las contraseñas no coinciden.')
      return
    }

    setSubmitting(true)
    try {
      const response = await authApi.register({ nombre, email, password })
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
          <span><strong>ClimaPlan</strong></span>
        </Link>
        <div className="auth-visual-copy">
          <span className="eyebrow eyebrow-light">CLIMAPLAN</span>
          <h1>Planes, destinos y clima en un solo lugar.</h1>
          <p>Crea tu cuenta para empezar a organizar tus actividades.</p>
        </div>
        <div className="sun-illustration" aria-hidden="true">
          <span className="sun-core" />
          <span className="sun-cloud cloud-one" />
          <span className="sun-cloud cloud-two" />
        </div>
        <span className="auth-visual-note">Cada destino tiene una historia por contar.</span>
      </section>

      <section className="auth-form-side">
        <div className="auth-card">
          <span className="eyebrow">EMPIEZA A PLANEAR</span>
          <h2>Crea tu cuenta</h2>
          <p className="muted">Solo necesitas unos datos para comenzar.</p>
          {error && <ErrorMessage message={error} />}

          <form className="form-stack" onSubmit={handleSubmit} aria-label="Crear cuenta">
            <label>
              Nombre completo
              <input
                autoComplete="name"
                name="nombre"
                value={nombre}
                onChange={(event) => setNombre(event.target.value)}
                placeholder="Tu nombre"
                required
              />
            </label>
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
                autoComplete="new-password"
                name="password"
                type="password"
                minLength={8}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Al menos 8 caracteres"
                required
              />
            </label>
            <label>
              Confirma tu contraseña
              <input
                autoComplete="new-password"
                name="confirmation"
                type="password"
                minLength={8}
                value={confirmation}
                onChange={(event) => setConfirmation(event.target.value)}
                placeholder="Repite tu contraseña"
                required
              />
            </label>
            <button className="button button-primary button-wide" disabled={submitting}>
              {submitting ? 'Creando cuenta...' : 'Crear cuenta'}
              {!submitting && <span aria-hidden="true">→</span>}
            </button>
          </form>

          <p className="auth-switch">
            ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
          </p>
        </div>
        <span className="auth-footer">ClimaPlan · planea mejor, viaja más</span>
      </section>
    </main>
  )
}
