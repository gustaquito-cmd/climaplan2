import { ErrorMessage, LoadingState } from './Feedback'
import type { PlanInvitation } from '../types'

interface PlanInvitationsProps {
  invitations: PlanInvitation[]
  loading: boolean
  error: string
  actionId: number | null
  onRefresh: () => void
  onAccept: (invitation: PlanInvitation) => void
  onReject: (invitation: PlanInvitation) => void
}

export function PlanInvitations({
  invitations,
  loading,
  error,
  actionId,
  onRefresh,
  onAccept,
  onReject,
}: PlanInvitationsProps) {
  return (
    <section className="data-section invitations-section" aria-label="Invitaciones pendientes">
      <div className="list-heading">
        <h2>Invitaciones pendientes <span className="count-badge">{invitations.length}</span></h2>
        <button
          className="button button-quiet"
          type="button"
          onClick={onRefresh}
          disabled={loading}
        >
          Actualizar
        </button>
      </div>
      {error && <ErrorMessage message={error} />}
      {loading ? (
        <LoadingState label="Cargando invitaciones..." />
      ) : invitations.length === 0 ? (
        <p className="muted">No tienes invitaciones pendientes.</p>
      ) : (
        <div className="invitation-list">
          {invitations.map((invitation) => (
            <article className="invitation-card" key={invitation.id}>
              <div>
                <p>{invitation.propietarioNombre ?? 'Un usuario'} te invitó a:</p>
                <h3>{invitation.tituloPlan}</h3>
                <small>{invitation.ciudad}</small>
              </div>
              <div className="card-actions">
                <button
                  className="button button-primary"
                  type="button"
                  onClick={() => onAccept(invitation)}
                  disabled={actionId !== null}
                >
                  {actionId === invitation.id ? 'Procesando...' : 'Aceptar'}
                </button>
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => onReject(invitation)}
                  disabled={actionId !== null}
                >
                  Rechazar
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
