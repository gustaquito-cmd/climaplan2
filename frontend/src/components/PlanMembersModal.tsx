import { Modal } from './Modal'
import { ErrorMessage, LoadingState } from './Feedback'
import type { Plan, PlanMember } from '../types'

interface PlanMembersModalProps {
  plan: Plan | null
  members: PlanMember[]
  loading: boolean
  owner: boolean
  email: string
  inviting: boolean
  error: string
  removingUserId: number | null
  onEmailChange: (email: string) => void
  onInvite: () => void
  onRemove: (member: PlanMember) => void
  onClose: () => void
}

function statusLabel(status: PlanMember['estado']) {
  switch (status) {
    case 'ACCEPTED':
      return 'Aceptado'
    case 'PENDING':
      return 'Pendiente'
    case 'REJECTED':
      return 'Rechazado'
  }
}

export function PlanMembersModal({
  plan,
  members,
  loading,
  owner,
  email,
  inviting,
  error,
  removingUserId,
  onEmailChange,
  onInvite,
  onRemove,
  onClose,
}: PlanMembersModalProps) {
  return (
    <Modal
      open={plan !== null}
      title="Participantes"
      onClose={onClose}
      closeOnOverlay={!inviting}
    >
      {plan && (
        <div className="plan-members-content">
          <p className="muted">{plan.titulo}</p>
          {loading ? (
            <LoadingState label="Cargando participantes..." />
          ) : (
            <>
              {error && <ErrorMessage message={error} />}
              <section aria-label="Propietario">
                <h3>Propietario</h3>
                {members.filter((member) => member.rol === 'OWNER').map((member) => (
                  <div className="plan-member-row" key={member.id}>
                    <span><strong>{member.nombre}</strong><small>{member.email}</small></span>
                    <span>Propietario</span>
                  </div>
                ))}
              </section>
              <section aria-label="Participantes">
                <h3>Participantes</h3>
                {members.filter((member) => member.rol === 'MEMBER').length === 0 ? (
                  <p className="muted">Aún no hay participantes invitados.</p>
                ) : (
                  members.filter((member) => member.rol === 'MEMBER').map((member) => (
                    <div className="plan-member-row" key={member.id}>
                      <span><strong>{member.nombre}</strong><small>{member.email}</small></span>
                      <span className="plan-member-actions">
                        {statusLabel(member.estado)}
                        {owner && member.estado === 'ACCEPTED' && (
                          <button
                            className="text-button danger-text"
                            type="button"
                            onClick={() => onRemove(member)}
                            disabled={removingUserId === member.id}
                          >
                            {removingUserId === member.id ? 'Eliminando...' : 'Eliminar'}
                          </button>
                        )}
                      </span>
                    </div>
                  ))
                )}
              </section>
              {owner && (
                <form
                  className="form-stack plan-member-invite"
                  onSubmit={(event) => {
                    event.preventDefault()
                    onInvite()
                  }}
                >
                  <label>
                    Correo electrónico
                    <input
                      type="email"
                      value={email}
                      onChange={(event) => onEmailChange(event.target.value)}
                      placeholder="usuario@correo.com"
                      required
                    />
                  </label>
                  <button className="button button-primary" type="submit" disabled={inviting}>
                    {inviting ? 'Enviando...' : 'Enviar invitación'}
                  </button>
                </form>
              )}
            </>
          )}
        </div>
      )}
    </Modal>
  )
}
