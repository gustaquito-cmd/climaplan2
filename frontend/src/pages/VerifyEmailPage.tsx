import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { authApi } from '../api/auth'
import { getApiErrorMessage } from '../api/errors'
import { ErrorMessage } from '../components/Feedback'

function getEmailFromState(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'email' in state) {
    return typeof state.email === 'string' ? state.email : ''
  }
  return ''
}

export function VerifyEmailPage() {
  const location = useLocation()
  const [email, setEmail] = useState(() => getEmailFromState(location.state))
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [verified, setVerified] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleVerify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const response = await authApi.verifyEmail({ email, code })
      setVerified(true)
      setNotice(response.message)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const response = await authApi.resendVerification({ email })
      setNotice(response.message)
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
          <span className="eyebrow eyebrow-light">UN PASO MÁS</span>
          <h1>Confirma tu correo para empezar a planear.</h1>
          <p>Te enviamos un código de seis dígitos que vence en cinco minutos.</p>
        </div>
        <span className="auth-visual-note">ClimaPlan · planea mejor, viaja más</span>
      </section>

      <section className="auth-form-side">
        <div className="auth-card">
          <span className="eyebrow">VERIFICACIÓN DE CORREO</span>
          <h2>Revisa tu bandeja de entrada</h2>
          <p className="muted">Ingresa el correo de tu cuenta y el código que recibiste.</p>

          {error && <ErrorMessage message={error} />}
          {notice && <div className="feedback feedback-info verification-notice" role="status">{notice}</div>}

          {verified ? (
            <Link
              className="button button-primary button-wide verification-login-link"
              to="/login"
              state={{ message: 'Tu correo está verificado. Ya puedes iniciar sesión.' }}
            >
              Ir a iniciar sesión
            </Link>
          ) : (
            <>
              <form className="form-stack" onSubmit={handleVerify} aria-label="Verificar correo">
                <label>
                  Correo electrónico
                  <input
                    autoComplete="email"
                    name="email"
                    type="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    required
                  />
                </label>
                <label>
                  Código de verificación
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
                  {submitting ? 'Verificando...' : 'Verificar correo'}
                </button>
              </form>

              <button
                className="text-button verification-resend"
                type="button"
                disabled={submitting || !email}
                onClick={handleResend}
              >
                Reenviar código
              </button>
            </>
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
