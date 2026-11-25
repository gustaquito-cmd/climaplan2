import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import { favoritesApi } from '../api/favorites'
import { plansApi } from '../api/plans'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon, type IconName } from '../components/Icon'
import { PlanInvitations } from '../components/PlanInvitations'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import type { PlanInvitation } from '../types'

const shortcuts: {
  to: string
  icon: IconName
  title: string
  description: string
  count?: 'plans' | 'favorites'
}[] = [
  {
    to: '/plans',
    icon: 'calendar',
    title: 'Planes',
    description: 'Organiza tus próximas actividades.',
    count: 'plans',
  },
  {
    to: '/favorites',
    icon: 'heart',
    title: 'Favoritos',
    description: 'Accede a tus destinos guardados.',
    count: 'favorites',
  },
  {
    to: '/weather',
    icon: 'cloud',
    title: 'Consultar clima',
    description: 'Revisa las condiciones actuales de una ciudad.',
  },
]

export function DashboardPage() {
  const { user, loading, profileError, reloadProfile } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [planCount, setPlanCount] = useState<number | null>(null)
  const [favoriteCount, setFavoriteCount] = useState<number | null>(null)
  const [countError, setCountError] = useState('')
  const [invitations, setInvitations] = useState<PlanInvitation[]>([])
  const [invitationsLoading, setInvitationsLoading] = useState(true)
  const [invitationsError, setInvitationsError] = useState('')
  const [invitationActionId, setInvitationActionId] = useState<number | null>(null)
  const handledAdminAccessDenied = useRef(false)
  const adminAccessDenied =
    typeof location.state === 'object' &&
    location.state !== null &&
    'adminAccessDenied' in location.state &&
    location.state.adminAccessDenied === true

  const loadInvitations = useCallback(async () => {
    setInvitationsLoading(true)
    setInvitationsError('')
    try {
      setInvitations(await plansApi.listInvitations())
    } catch (requestError) {
      setInvitationsError(getApiErrorMessage(requestError))
    } finally {
      setInvitationsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!adminAccessDenied || handledAdminAccessDenied.current) return
    handledAdminAccessDenied.current = true
    showToast('No tienes permisos para acceder a esta sección.')
    navigate('/dashboard', { replace: true, state: null })
  }, [adminAccessDenied, navigate, showToast])

  useEffect(() => {
    let active = true
    void Promise.allSettled([plansApi.list(), favoritesApi.list()]).then((results) => {
      if (!active) return
      const [plans, favorites] = results
      if (plans.status === 'fulfilled') setPlanCount(plans.value.length)
      if (favorites.status === 'fulfilled') setFavoriteCount(favorites.value.length)
      if (plans.status === 'rejected' || favorites.status === 'rejected') {
        setCountError('No se pudieron cargar todos los contadores.')
      }
    })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    void loadInvitations()
  }, [loadInvitations])

  async function resolveInvitation(invitation: PlanInvitation, accept: boolean) {
    setInvitationActionId(invitation.id)
    setInvitationsError('')
    try {
      if (accept) {
        await plansApi.acceptInvitation(invitation.id)
        showToast('Invitación aceptada. El plan ya está en Mis planes.')
        try {
          const plans = await plansApi.list()
          setPlanCount(plans.length)
          setCountError('')
        } catch {
          setCountError('Invitación aceptada, pero no se pudo actualizar el contador de planes.')
        }
      } else {
        await plansApi.rejectInvitation(invitation.id)
        showToast('Invitación rechazada.')
      }
      await loadInvitations()
    } catch (requestError) {
      setInvitationsError(getApiErrorMessage(requestError))
    } finally {
      setInvitationActionId(null)
    }
  }

  if (loading) return <LoadingState label="Cargando tu perfil..." />
  if (profileError) {
    return (
      <section>
        <ErrorMessage message={profileError} />
        <button className="button button-secondary" onClick={() => void reloadProfile()}>
          Reintentar
        </button>
      </section>
    )
  }

  return (
    <section>
      <header className="dashboard-welcome">
        <div>
          <span className="eyebrow">INICIO</span>
          <h1>Hola, {user?.nombre?.split(' ')[0] ?? 'Gustavo'}</h1>
          <p>Accede a tus planes, destinos favoritos y consultas del clima.</p>
        </div>
      </header>

      <div className="section-heading">
        <h2>Accesos rápidos</h2>
      </div>
      {countError && <ErrorMessage message={countError} />}
      <div className="shortcut-grid">
        {shortcuts.map((shortcut) => {
          const count = shortcut.count === 'plans'
            ? planCount
            : shortcut.count === 'favorites'
              ? favoriteCount
              : null
          return (
            <Link className="shortcut-card" to={shortcut.to} key={shortcut.to}>
              <span className="shortcut-icon"><Icon name={shortcut.icon} size={19} /></span>
              <span className="shortcut-copy">
                <strong>{shortcut.title}</strong>
                <small>{shortcut.description}</small>
              </span>
              {count !== null && <span className="shortcut-count" aria-label={`${count} elementos`}>{count ?? '—'}</span>}
            </Link>
          )
        })}
      </div>

      <PlanInvitations
        invitations={invitations}
        loading={invitationsLoading}
        error={invitationsError}
        actionId={invitationActionId}
        onRefresh={() => void loadInvitations()}
        onAccept={(invitation) => void resolveInvitation(invitation, true)}
        onReject={(invitation) => void resolveInvitation(invitation, false)}
      />
    </section>
  )
}
