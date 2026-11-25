import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { plansApi } from '../api/plans'
import { ConfirmModal } from '../components/ConfirmModal'
import { EmptyState, ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { PageHeader } from '../components/PageHeader'
import { PlanCard } from '../components/PlanCard'
import { PlanMembersModal } from '../components/PlanMembersModal'
import { PlanStatusField } from '../components/PlanStatusField'
import { WeatherResult } from '../components/WeatherResult'
import { useToast } from '../context/ToastContext'
import type {
  Plan,
  PlanCreateInput,
  PlanInput,
  PlanShareStatus,
  PlanWeatherCheck,
  PlanMember,
} from '../types'

const emptyPlan = (): PlanInput => ({
  titulo: '',
  ciudad: '',
  fecha: new Date().toISOString().slice(0, 10),
  tipoActividad: '',
  notas: '',
  estado: 'PENDIENTE',
})

function statusLabel(status: Plan['estado']) {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function PlansPage() {
  const { showToast } = useToast()
  const navigate = useNavigate()
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
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(() => new Set())
  const [favoritesLoading, setFavoritesLoading] = useState(true)
  const [favoriteLoadError, setFavoriteLoadError] = useState('')
  const [favoriteError, setFavoriteError] = useState('')
  const [pendingFavoriteIds, setPendingFavoriteIds] = useState<Set<number>>(() => new Set())
  const pendingFavorites = useRef(new Set<number>())
  const [formOpen, setFormOpen] = useState(false)
  const [viewPlan, setViewPlan] = useState<Plan | null>(null)
  const [deletePlan, setDeletePlan] = useState<Plan | null>(null)
  const [weatherCheck, setWeatherCheck] = useState<PlanWeatherCheck | null>(null)
  const [weatherError, setWeatherError] = useState('')
  const [shareDialogPlan, setShareDialogPlan] = useState<Plan | null>(null)
  const [shareDetails, setShareDetails] = useState<PlanShareStatus | null>(null)
  const [shareLoading, setShareLoading] = useState(false)
  const [shareSaving, setShareSaving] = useState(false)
  const [shareRevoking, setShareRevoking] = useState(false)
  const [shareError, setShareError] = useState('')
  const [confirmShareRevocation, setConfirmShareRevocation] = useState(false)
  const [membersPlan, setMembersPlan] = useState<Plan | null>(null)
  const [members, setMembers] = useState<PlanMember[]>([])
  const [membersLoading, setMembersLoading] = useState(false)
  const [membersError, setMembersError] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [invitingMember, setInvitingMember] = useState(false)
  const [removingMember, setRemovingMember] = useState<PlanMember | null>(null)
  const [removingMemberId, setRemovingMemberId] = useState<number | null>(null)

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

  const loadFavorites = useCallback(async () => {
    setFavoritesLoading(true)
    setFavoriteLoadError('')
    try {
      setFavoriteIds(new Set(await plansApi.listFavoriteIds()))
    } catch (requestError) {
      setFavoriteLoadError(getApiErrorMessage(requestError))
    } finally {
      setFavoritesLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadFavorites()
  }, [loadFavorites])

  async function toggleFavorite(plan: Plan) {
    if (pendingFavorites.current.has(plan.id) || favoritesLoading || favoriteLoadError) return
    pendingFavorites.current.add(plan.id)
    setPendingFavoriteIds(new Set(pendingFavorites.current))
    setFavoriteError('')
    try {
      const isFavorite = favoriteIds.has(plan.id)
      if (isFavorite) {
        await plansApi.removeFavorite(plan.id)
      } else {
        await plansApi.addFavorite(plan.id)
      }
      setFavoriteIds((current) => {
        const next = new Set(current)
        if (isFavorite) next.delete(plan.id)
        else next.add(plan.id)
        return next
      })
    } catch (requestError) {
      setFavoriteError(getApiErrorMessage(requestError))
    } finally {
      pendingFavorites.current.delete(plan.id)
      setPendingFavoriteIds(new Set(pendingFavorites.current))
    }
  }

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
        const createInput: PlanCreateInput = {
          titulo: form.titulo,
          ciudad: form.ciudad,
          fecha: form.fecha,
          tipoActividad: form.tipoActividad,
          notas: form.notas,
        }
        await plansApi.create(createInput)
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

  async function openShare(plan: Plan) {
    setShareDialogPlan(plan)
    setShareDetails(null)
    setShareError('')
    setShareLoading(true)
    try {
      setShareDetails(await plansApi.getShare(plan.id))
    } catch (requestError) {
      setShareError(getApiErrorMessage(requestError))
    } finally {
      setShareLoading(false)
    }
  }

  function closeShareDialog() {
    if (shareSaving || shareRevoking) return
    setShareDialogPlan(null)
    setShareDetails(null)
    setShareError('')
  }

  async function generateShare() {
    if (!shareDialogPlan) return
    setShareSaving(true)
    setShareError('')
    try {
      setShareDetails(await plansApi.createShare(shareDialogPlan.id))
      showToast('Enlace público generado.')
    } catch (requestError) {
      setShareError(getApiErrorMessage(requestError))
    } finally {
      setShareSaving(false)
    }
  }

  async function copyShareLink() {
    if (!shareDetails?.token) return
    const shareUrl = `${window.location.origin}/shared/${shareDetails.token}`
    try {
      if (!navigator.clipboard?.writeText) {
        throw new Error('Clipboard API no disponible')
      }
      await navigator.clipboard.writeText(shareUrl)
      showToast('Enlace copiado')
      setShareError('')
    } catch {
      setShareError('No se pudo copiar el enlace. Puedes seleccionarlo y copiarlo manualmente.')
    }
  }

  async function revokeShare() {
    if (!shareDialogPlan) return
    setShareRevoking(true)
    setShareError('')
    try {
      await plansApi.revokeShare(shareDialogPlan.id)
      setShareDetails({ token: null, active: false, createdAt: null, expiresAt: null })
      setConfirmShareRevocation(false)
      showToast('Enlace revocado.')
    } catch (requestError) {
      setShareError(getApiErrorMessage(requestError))
      setConfirmShareRevocation(false)
    } finally {
      setShareRevoking(false)
    }
  }

  async function openMembers(plan: Plan) {
    setMembersPlan(plan)
    setMembers([])
    setInviteEmail('')
    setMembersError('')
    setMembersLoading(true)
    try {
      setMembers(await plansApi.listMembers(plan.id))
    } catch (requestError) {
      setMembersError(getApiErrorMessage(requestError))
    } finally {
      setMembersLoading(false)
    }
  }

  async function inviteMember() {
    if (!membersPlan) return
    setInvitingMember(true)
    setMembersError('')
    try {
      await plansApi.inviteMember(membersPlan.id, inviteEmail)
      setInviteEmail('')
      setMembers(await plansApi.listMembers(membersPlan.id))
      showToast('Invitación enviada.')
    } catch (requestError) {
      setMembersError(getApiErrorMessage(requestError))
    } finally {
      setInvitingMember(false)
    }
  }

  async function confirmRemoveMember() {
    if (!membersPlan || !removingMember) return
    setRemovingMemberId(removingMember.id)
    setMembersError('')
    try {
      await plansApi.removeMember(membersPlan.id, removingMember.id)
      setMembers(await plansApi.listMembers(membersPlan.id))
      setRemovingMember(null)
      showToast('Participante eliminado.')
    } catch (requestError) {
      setMembersError(getApiErrorMessage(requestError))
      setRemovingMember(null)
    } finally {
      setRemovingMemberId(null)
    }
  }

  function closeMembers() {
    if (invitingMember || removingMemberId !== null) return
    setMembersPlan(null)
    setMembers([])
    setMembersError('')
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
      {favoriteError && <ErrorMessage message={favoriteError} />}
      {favoriteLoadError && (
        <div>
          <ErrorMessage message={`No se pudieron cargar los favoritos: ${favoriteLoadError}`} />
          <button className="text-button" type="button" onClick={() => void loadFavorites()}>
            Reintentar favoritos
          </button>
        </div>
      )}
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
              <PlanCard
                key={plan.id}
                plan={plan}
                loadingPlan={loadingPlanId === plan.id}
                loadingWeather={loadingWeatherId === plan.id}
                isFavorite={favoriteIds.has(plan.id)}
                favoriteLoading={pendingFavoriteIds.has(plan.id)}
                favoriteUnavailable={favoritesLoading || Boolean(favoriteLoadError)}
                onToggleFavorite={(item) => void toggleFavorite(item)}
                onView={setViewPlan}
                onEdit={(item) => void beginEdit(item.id)}
                onWeather={(item) => void checkWeather(item)}
                onShare={(item) => void openShare(item)}
                onMembers={(item) => void openMembers(item)}
                onChat={(item) => navigate(`/plans/${item.id}/chat`)}
                onDelete={setDeletePlan}
              />
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
          <PlanStatusField
            editing={editingId !== null}
            value={form.estado}
            onChange={(value) => updateField('estado', value)}
          />
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
          <WeatherResult
            weatherDataType={weatherCheck.weatherDataType}
            fecha={weatherCheck.fecha}
            ciudad={weatherCheck.ciudad}
            estadoClimatico={weatherCheck.estadoClimatico}
            clima={weatherCheck.clima}
            recomendacion={weatherCheck.recomendacion}
            mensaje={weatherCheck.mensaje}
          />
        ) : null}
      </Modal>

      <Modal
        open={shareDialogPlan !== null}
        title="Compartir plan"
        onClose={closeShareDialog}
        closeOnOverlay={!shareSaving && !shareRevoking}
      >
        {shareLoading ? (
          <LoadingState label="Consultando enlace..." />
        ) : (
          <>
            <p className="confirm-message">
              {shareDialogPlan
                ? `Cualquier persona con el enlace podrá consultar “${shareDialogPlan.titulo}” en modo de solo lectura.`
                : ''}
            </p>
            {shareError && <ErrorMessage message={shareError} />}
            {shareDetails?.active && shareDetails.token ? (
              <div className="share-link-content">
                <label className="share-link-field">
                  Enlace público
                  <input
                    aria-label="Enlace público"
                    readOnly
                    value={`${window.location.origin}/shared/${shareDetails.token}`}
                    onFocus={(event) => event.currentTarget.select()}
                  />
                </label>
                <div className="share-link-actions">
                  <button className="button button-primary" type="button" onClick={() => void copyShareLink()}>
                    Copiar enlace
                  </button>
                  <button
                    className="button button-secondary"
                    type="button"
                    onClick={() => setConfirmShareRevocation(true)}
                    disabled={shareRevoking}
                  >
                    Revocar enlace
                  </button>
                </div>
              </div>
            ) : shareDetails && (
              <div className="share-link-actions share-create-action">
                <button
                  className="button button-primary"
                  type="button"
                  onClick={() => void generateShare()}
                  disabled={shareSaving}
                >
                  {shareSaving ? 'Generando...' : 'Generar enlace'}
                </button>
              </div>
            )}
          </>
        )}
      </Modal>

      <ConfirmModal
        open={deletePlan !== null}
        title="Eliminar plan"
        message={deletePlan ? `¿Quieres eliminar “${deletePlan.titulo}”? Esta acción no se puede deshacer.` : ''}
        loading={removing}
        onConfirm={() => void confirmRemove()}
        onClose={() => setDeletePlan(null)}
      />

      <ConfirmModal
        open={confirmShareRevocation}
        title="Revocar enlace"
        message="Las personas que tengan este enlace dejarán de poder consultar el plan."
        confirmLabel="Revocar enlace"
        loadingLabel="Revocando..."
        loading={shareRevoking}
        onConfirm={() => void revokeShare()}
        onClose={() => {
          if (!shareRevoking) setConfirmShareRevocation(false)
        }}
      />

      <PlanMembersModal
        plan={membersPlan}
        members={members}
        loading={membersLoading}
        owner={membersPlan?.ownership === 'OWNER'}
        email={inviteEmail}
        inviting={invitingMember}
        error={membersError}
        removingUserId={removingMemberId}
        onEmailChange={setInviteEmail}
        onInvite={() => void inviteMember()}
        onRemove={setRemovingMember}
        onClose={closeMembers}
      />

      <ConfirmModal
        open={removingMember !== null}
        title="Eliminar participante"
        message={removingMember
          ? `¿Quieres eliminar a ${removingMember.nombre} de este plan?`
          : ''}
        confirmLabel="Eliminar participante"
        loadingLabel="Eliminando..."
        loading={removingMemberId !== null}
        onConfirm={() => void confirmRemoveMember()}
        onClose={() => {
          if (removingMemberId === null) setRemovingMember(null)
        }}
      />
    </section>
  )
}
