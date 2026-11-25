import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { ErrorMessage } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { verifyTwoFactorAndAuthenticate } from '../utils/authFlow'

function getChallengeId(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'challengeId' in state) {
    return typeof state.challengeId === 'string' ? state.challengeId : ''
  }
  return ''
}

export function VerifyTwoFactorPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { setAuthenticated } = useAuth()
  const challengeId = getChallengeId(location.state)
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await verifyTwoFactorAndAuthenticate(
        challengeId,
        code,
        authApi.verifyTwoFactor,
        setAuthenticated,
      )
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
          <span className="eyebrow eyebrow-light">SEGUNDO FACTOR</span>
          <h1>Confirma que eres tú.</h1>
          <p>Ingresa el código de acceso que enviamos a tu correo.</p>
        </div>
        <span className="auth-visual-note">ClimaPlan · planea mejor, viaja más</span>
      </section>

      <section className="auth-form-side">
        <div className="auth-card">
          <span className="eyebrow">VERIFICACIÓN 2FA</span>
          <h2>Código de acceso</h2>
          <p className="muted">El código vence en cinco minutos.</p>
          {error && <ErrorMessage message={error} />}

          {challengeId ? (
            <form className="form-stack" onSubmit={handleSubmit} aria-label="Verificar segundo factor">
              <label>
                Código de seis dígitos
                <input
                  autoComplete="one-time-code"
                  inputMode="numeric"
                  name="code"
                  pattern="[0-9]{6}"
                  maxLength={6}
                  value={code}
                  onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                  placeholder="123456"
                  required
                />
              </label>
              <button className="button button-primary button-wide" disabled={submitting}>
                {submitting ? 'Verificando...' : 'Verificar'}
              </button>
            </form>
          ) : (
            <div className="feedback feedback-info" role="status">
              No hay una verificación pendiente. Inicia sesión para solicitar un código nuevo.
            </div>
          )}

          <p className="auth-switch">
            <Link to="/login">Volver a iniciar sesión</Link>
          </p>
        </div>
        <span className="auth-footer">ClimaPlan · planea mejor, viaja más</span>
      </section>
    </main>
  )
}
