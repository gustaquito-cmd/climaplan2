import axios from 'axios'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { plansApi } from '../api/plans'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import { WeatherResult } from '../components/WeatherResult'
import type { SharedPlan } from '../types'

function statusLabel(status: SharedPlan['estado']) {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function SharedPlanPage() {
  const { token } = useParams<{ token: string }>()
  const [plan, setPlan] = useState<SharedPlan | null>(null)
  const [loading, setLoading] = useState(true)
  const [unavailable, setUnavailable] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setUnavailable(false)
    setError('')

    if (!token) {
      setUnavailable(true)
      setLoading(false)
      return () => {
        active = false
      }
    }

    void plansApi.getSharedPlan(token)
      .then((response) => {
        if (active) setPlan(response)
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (axios.isAxiosError(requestError) && requestError.response?.status === 404) {
          setUnavailable(true)
        } else {
          setError(getApiErrorMessage(requestError))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [token])

  return (
    <main className="shared-plan-page">
      <header className="shared-plan-header">
        <Link className="brand" to="/" aria-label="ClimaPlan">
          <span className="brand-mark">C</span>
          <strong>ClimaPlan</strong>
        </Link>
        <span className="shared-plan-label">Plan compartido</span>
      </header>

      <div className="shared-plan-content">
        {loading ? (
          <LoadingState label="Cargando plan compartido..." />
        ) : unavailable ? (
          <section className="shared-plan-unavailable" role="status">
            <span className="shared-plan-icon" aria-hidden="true">C</span>
            <h1>Este enlace ya no está disponible.</h1>
            <p>Es posible que el propietario lo haya revocado o que haya expirado.</p>
          </section>
        ) : error ? (
          <ErrorMessage message={error} />
        ) : plan ? (
          <article className="shared-plan-card">
            <p className="eyebrow">PLAN COMPARTIDO</p>
            <div className="shared-plan-title-row">
              <div>
                <h1>{plan.titulo}</h1>
                <p className="shared-plan-location">{plan.ciudad} · {plan.tipoActividad}</p>
              </div>
              <span className={`status-badge status-${plan.estado.toLowerCase()}`}>
                {statusLabel(plan.estado)}
              </span>
            </div>
            <dl className="detail-list shared-plan-details">
              <div><dt>Ciudad</dt><dd>{plan.ciudad}</dd></div>
              <div><dt>Fecha</dt><dd>{plan.fecha}</dd></div>
              <div><dt>Actividad</dt><dd>{plan.tipoActividad}</dd></div>
            </dl>
            <section className="shared-plan-weather" aria-label="Evaluación climática">
              <WeatherResult
                weatherDataType={plan.weatherDataType}
                fecha={plan.fecha}
                ciudad={plan.ciudad}
                estadoClimatico={plan.estadoClimatico}
                clima={plan.clima}
                recomendacion={plan.recomendacion}
                mensaje={plan.avisoClimatico}
                publicView
              />
            </section>
          </article>
        ) : null}
      </div>
    </main>
  )
}
