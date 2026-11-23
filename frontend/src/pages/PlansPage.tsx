import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { plansApi } from '../api/plans'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import type { Plan, PlanInput, PlanWeatherCheck } from '../types'

const emptyPlan = (): PlanInput => ({
  titulo: '',
  ciudad: '',
  fecha: new Date().toISOString().slice(0, 10),
  tipoActividad: '',
  notas: '',
  estado: 'PENDIENTE',
})

function isPlanStatus(value: string): value is PlanInput['estado'] {
  return value === 'PENDIENTE' || value === 'COMPLETADO' || value === 'CANCELADO'
}

export function PlansPage() {
  const [plans, setPlans] = useState<Plan[]>([])
  const [form, setForm] = useState<PlanInput>(emptyPlan)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [weatherCheck, setWeatherCheck] = useState<PlanWeatherCheck | null>(null)

  const loadPlans = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      setPlans(await plansApi.list())
    } catch (requestError) {
      setLoadError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadPlans()
  }, [loadPlans])

  function updateField<K extends keyof PlanInput>(field: K, value: PlanInput[K]) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (editingId === null) {
        await plansApi.create(form)
      } else {
        await plansApi.update(editingId, form)
      }
      setForm(emptyPlan())
      setEditingId(null)
      setWeatherCheck(null)
      await loadPlans()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function beginEdit(id: number) {
    setError('')
    try {
      const plan = await plansApi.get(id)
      setForm({
        titulo: plan.titulo,
        ciudad: plan.ciudad,
        fecha: plan.fecha,
        tipoActividad: plan.tipoActividad,
        notas: plan.notas ?? '',
        estado: plan.estado,
      })
      setEditingId(plan.id)
      setWeatherCheck(null)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  async function removePlan(plan: Plan) {
    if (!window.confirm(`¿Eliminar el plan "${plan.titulo}"?`)) return
    setError('')
    try {
      await plansApi.remove(plan.id)
      await loadPlans()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  async function checkWeather(id: number) {
    setError('')
    setWeatherCheck(null)
    try {
      setWeatherCheck(await plansApi.weatherCheck(id))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(emptyPlan())
    setError('')
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <span className="eyebrow">ORGANIZA TU PRÓXIMA SALIDA</span>
          <h1>Mis planes</h1>
          <p>Guarda tus actividades y consulta el clima antes de salir.</p>
        </div>
        <span className="heading-stamp" aria-hidden="true">✈</span>
      </div>

      <div className="content-grid">
        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">{editingId === null ? 'NUEVO PLAN' : 'EDITAR PLAN'}</span>
              <h2>{editingId === null ? 'Agrega una idea' : 'Actualiza tu plan'}</h2>
            </div>
            <span className="panel-icon" aria-hidden="true">＋</span>
          </div>
          {error && <ErrorMessage message={error} />}
          <form className="form-stack" onSubmit={handleSubmit}>
            <label>
              Título
              <input
                value={form.titulo}
                onChange={(event) => updateField('titulo', event.target.value)}
                placeholder="Ej. Caminata por el centro"
                maxLength={120}
                required
              />
            </label>
            <div className="form-row">
              <label>
                Ciudad
                <input
                  value={form.ciudad}
                  onChange={(event) => updateField('ciudad', event.target.value)}
                  placeholder="Ciudad destino"
                  required
                />
              </label>
              <label>
                Fecha
                <input
                  type="date"
                  value={form.fecha}
                  onChange={(event) => updateField('fecha', event.target.value)}
                  required
                />
              </label>
            </div>
            <label>
              Tipo de actividad
              <input
                value={form.tipoActividad}
                onChange={(event) => updateField('tipoActividad', event.target.value)}
                placeholder="Ej. Senderismo"
                required
              />
            </label>
            <label>
              Notas <span className="optional-label">opcional</span>
              <textarea
                rows={3}
                value={form.notas}
                onChange={(event) => updateField('notas', event.target.value)}
                placeholder="¿Qué necesitas recordar?"
              />
            </label>
            <label>
              Estado
              <select
                value={form.estado}
                onChange={(event) => {
                  if (isPlanStatus(event.target.value)) {
                    updateField('estado', event.target.value)
                  }
                }}
              >
                <option value="PENDIENTE">Pendiente</option>
                <option value="COMPLETADO">Completado</option>
                <option value="CANCELADO">Cancelado</option>
              </select>
            </label>
            <div className="form-actions">
              <button className="button button-primary" disabled={saving}>
                {saving ? 'Guardando...' : editingId === null ? 'Crear plan' : 'Guardar cambios'}
              </button>
              {editingId !== null && (
                <button className="button button-secondary" type="button" onClick={cancelEdit}>
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="plans-column">
          <div className="list-heading">
            <div>
              <span className="eyebrow">TU AGENDA</span>
              <h2>Planes guardados <span className="count-badge">{plans.length}</span></h2>
            </div>
            <button className="button button-quiet" onClick={() => void loadPlans()} type="button">
              Actualizar
            </button>
          </div>
          {loadError && <ErrorMessage message={loadError} />}
          {loading ? (
            <LoadingState label="Cargando tus planes..." />
          ) : plans.length === 0 ? (
            <div className="empty-state">
              <span aria-hidden="true">🧭</span>
              <h3>Aún no tienes planes</h3>
              <p>Agrega una idea en el formulario para verla aquí.</p>
            </div>
          ) : (
            <div className="plan-list">
              {plans.map((plan) => (
                <article className="plan-card" key={plan.id}>
                  <div className="plan-card-top">
                    <span className="date-chip">{plan.fecha}</span>
                    <span className={`status-badge status-${plan.estado.toLowerCase()}`}>
                      {plan.estado.toLowerCase()}
                    </span>
                  </div>
                  <h3>{plan.titulo}</h3>
                  <p className="plan-location"><span aria-hidden="true">⌖</span> {plan.ciudad} <span>·</span> {plan.tipoActividad}</p>
                  {plan.notas && <p className="plan-notes">{plan.notas}</p>}
                  <div className="card-actions">
                    <button className="text-button" onClick={() => void checkWeather(plan.id)} type="button">
                      ☼ Revisar clima
                    </button>
                    <button className="text-button" onClick={() => void beginEdit(plan.id)} type="button">
                      Editar
                    </button>
                    <button className="text-button danger-text" onClick={() => void removePlan(plan)} type="button">
                      Eliminar
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}

          {weatherCheck && (
            <article className="weather-check-card">
              <div className="weather-check-top">
                <div>
                  <span className="eyebrow">CLIMA PARA TU PLAN</span>
                  <h3>{weatherCheck.ciudad} · {weatherCheck.fecha}</h3>
                </div>
                <span className={`weather-status weather-${weatherCheck.estadoClimatico.toLowerCase()}`}>
                  {weatherCheck.estadoClimatico.replaceAll('_', ' ')}
                </span>
              </div>
              <p>{weatherCheck.recomendacion}</p>
              <span className="muted">
                {weatherCheck.clima.temperatura}°C · {weatherCheck.clima.descripcion}
              </span>
            </article>
          )}
        </section>
      </div>
    </section>
  )
}
