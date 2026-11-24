import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { plansApi } from '../api/plans'
import { ConfirmModal } from '../components/ConfirmModal'
import { EmptyState, ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../context/ToastContext'
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

function statusLabel(status: Plan['estado']) {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function PlansPage() {
  const { showToast } = useToast()
  const [plans, setPlans] = useState<Plan[]>([])
  const [form, setForm] = useState<PlanInput>(emptyPlan)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [removing, setRemoving] = useState(false)
  const [loadingPlanId, setLoadingPlanId] = useState<number | null>(null)
  const [loadingWeatherId, setLoadingWeatherId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [viewPlan, setViewPlan] = useState<Plan | null>(null)
  const [deletePlan, setDeletePlan] = useState<Plan | null>(null)
  const [weatherCheck, setWeatherCheck] = useState<PlanWeatherCheck | null>(null)
  const [weatherError, setWeatherError] = useState('')

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

  function openCreate() {
    setForm(emptyPlan())
    setEditingId(null)
    setError('')
    setFormOpen(true)
  }

  function closeForm() {
    if (saving) return
    setFormOpen(false)
    setError('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const updating = editingId !== null
      if (editingId === null) {
        await plansApi.create(form)
      } else {
        await plansApi.update(editingId, form)
      }
      setFormOpen(false)
      setEditingId(null)
      showToast(updating ? 'Plan actualizado correctamente.' : 'Plan creado correctamente.')
      await loadPlans()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function beginEdit(id: number) {
    setError('')
    setLoadingPlanId(id)
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
      setFormOpen(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoadingPlanId(null)
    }
  }

  async function confirmRemove() {
    if (!deletePlan) return
    setRemoving(true)
    setError('')
    try {
      await plansApi.remove(deletePlan.id)
      setDeletePlan(null)
      showToast('Plan eliminado correctamente.')
      await loadPlans()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
      setDeletePlan(null)
    } finally {
      setRemoving(false)
    }
  }

  async function checkWeather(plan: Plan) {
    setWeatherCheck(null)
    setWeatherError('')
    setViewPlan(null)
    setLoadingWeatherId(plan.id)
    setWeatherModalOpen(true)
    try {
      setWeatherCheck(await plansApi.weatherCheck(plan.id))
    } catch (requestError) {
      setWeatherError(getApiErrorMessage(requestError))
    } finally {
      setLoadingWeatherId(null)
    }
  }

  const [weatherModalOpen, setWeatherModalOpen] = useState(false)

  return (
    <section>
      <PageHeader
        title="Mis planes"
        description="Administra tus actividades y consulta el clima antes de salir."
        action={
          <button className="button button-primary" type="button" onClick={openCreate}>
            <Icon name="plus" size={17} /> Nuevo plan
          </button>
        }
      />

      {error && <ErrorMessage message={error} />}
      {loadError && <ErrorMessage message={loadError} />}
      {loading ? (
        <LoadingState label="Cargando planes..." />
      ) : plans.length === 0 ? (
        <EmptyState
          title="Aún no tienes planes"
          description="Crea un plan para guardar una actividad y revisar sus condiciones."
        />
      ) : (
        <section className="data-section" aria-label="Planes guardados">
          <div className="list-heading">
            <h2>Planes <span className="count-badge">{plans.length}</span></h2>
            <button className="button button-quiet" onClick={() => void loadPlans()} type="button" disabled={loading}>
              Actualizar
            </button>
          </div>
          <div className="plan-list">
            {plans.map((plan) => (
              <article className="plan-card" key={plan.id}>
                <div className="plan-summary">
                  <div className="plan-card-top">
                    <span className="date-chip"><Icon name="calendar" size={14} />{plan.fecha}</span>
                    <span className={`status-badge status-${plan.estado.toLowerCase()}`}>
                      {statusLabel(plan.estado)}
                    </span>
                  </div>
                  <h3>{plan.titulo}</h3>
                  <p className="plan-location">
                    <Icon name="pin" size={15} />{plan.ciudad}<span aria-hidden="true">·</span>{plan.tipoActividad}
                  </p>
                </div>
                <div className="card-actions">
                  <button className="text-button" onClick={() => setViewPlan(plan)} type="button" aria-label={`Ver ${plan.titulo}`}>
                    <Icon name="eye" size={15} /> Ver
                  </button>
                  <button
                    className="text-button"
                    onClick={() => void beginEdit(plan.id)}
                    type="button"
                    disabled={loadingPlanId === plan.id}
                  >
                    <Icon name="edit" size={15} /> {loadingPlanId === plan.id ? 'Cargando...' : 'Editar'}
                  </button>
                  <button
                    className="text-button"
                    onClick={() => void checkWeather(plan)}
                    type="button"
                    disabled={loadingWeatherId === plan.id}
                  >
                    <Icon name="cloud" size={15} /> Clima
                  </button>
                  <button className="text-button danger-text" onClick={() => setDeletePlan(plan)} type="button">
                    <Icon name="trash" size={15} /> Eliminar
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      <Modal
        open={formOpen}
        title={editingId === null ? 'Nuevo plan' : 'Editar plan'}
        onClose={closeForm}
        closeOnOverlay={!saving}
        footer={
          <>
            <button className="button button-secondary" type="button" onClick={closeForm} disabled={saving}>Cancelar</button>
            <button className="button button-primary" type="submit" form="plan-form" disabled={saving}>
              {saving ? 'Guardando...' : editingId === null ? 'Crear plan' : 'Guardar cambios'}
            </button>
          </>
        }
      >
        {error && <ErrorMessage message={error} />}
        <form id="plan-form" className="form-stack" onSubmit={handleSubmit}>
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
              <input value={form.ciudad} onChange={(event) => updateField('ciudad', event.target.value)} required />
            </label>
            <label>
              Fecha
              <input type="date" value={form.fecha} onChange={(event) => updateField('fecha', event.target.value)} required />
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
            <textarea rows={3} value={form.notas} onChange={(event) => updateField('notas', event.target.value)} />
          </label>
          <label>
            Estado
            <select
              value={form.estado}
              onChange={(event) => {
                if (isPlanStatus(event.target.value)) updateField('estado', event.target.value)
              }}
            >
              <option value="PENDIENTE">Pendiente</option>
              <option value="COMPLETADO">Completado</option>
              <option value="CANCELADO">Cancelado</option>
            </select>
          </label>
        </form>
      </Modal>

      <Modal open={viewPlan !== null} title="Detalle del plan" onClose={() => setViewPlan(null)}>
        {viewPlan && (
          <dl className="detail-list">
            <div><dt>Título</dt><dd>{viewPlan.titulo}</dd></div>
            <div><dt>Ciudad</dt><dd>{viewPlan.ciudad}</dd></div>
            <div><dt>Fecha</dt><dd>{viewPlan.fecha}</dd></div>
            <div><dt>Actividad</dt><dd>{viewPlan.tipoActividad}</dd></div>
            <div><dt>Estado</dt><dd>{statusLabel(viewPlan.estado)}</dd></div>
            {viewPlan.notas && <div><dt>Notas</dt><dd>{viewPlan.notas}</dd></div>}
          </dl>
        )}
      </Modal>

      <Modal
        open={weatherModalOpen}
        title="Clima para el plan"
        onClose={() => setWeatherModalOpen(false)}
      >
        {loadingWeatherId !== null ? (
          <LoadingState label="Consultando weather-check..." />
        ) : weatherError ? (
          <ErrorMessage message={weatherError} />
        ) : weatherCheck ? (
          <div className="weather-check-detail">
            <div className="weather-check-top">
              <div>
                <span className="muted">{weatherCheck.fecha}</span>
                <h3>{weatherCheck.ciudad}</h3>
              </div>
              <span className={`weather-status weather-${weatherCheck.estadoClimatico.toLowerCase()}`}>
                {weatherCheck.estadoClimatico.replaceAll('_', ' ').toLowerCase()}
              </span>
            </div>
            <p className="weather-description">{weatherCheck.clima.descripcion}</p>
            <div className="weather-metrics">
              <div><span>Temperatura</span><strong>{weatherCheck.clima.temperatura}°C</strong></div>
              <div><span>Humedad</span><strong>{weatherCheck.clima.humedad}%</strong></div>
              <div><span>Viento</span><strong>{weatherCheck.clima.velocidadViento} m/s</strong></div>
            </div>
            <div className="recommendation"><strong>Recomendación</strong><p>{weatherCheck.recomendacion}</p></div>
          </div>
        ) : null}
      </Modal>

      <ConfirmModal
        open={deletePlan !== null}
        title="Eliminar plan"
        message={deletePlan ? `¿Quieres eliminar “${deletePlan.titulo}”? Esta acción no se puede deshacer.` : ''}
        loading={removing}
        onConfirm={() => void confirmRemove()}
        onClose={() => setDeletePlan(null)}
      />
    </section>
  )
}
