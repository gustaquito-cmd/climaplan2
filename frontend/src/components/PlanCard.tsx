import { Icon } from './Icon'
import type { Plan } from '../types'

interface PlanCardProps {
  plan: Plan
  loadingPlan: boolean
  loadingWeather: boolean
  isFavorite: boolean
  favoriteLoading: boolean
  favoriteUnavailable: boolean
  onToggleFavorite: (plan: Plan) => void
  onView: (plan: Plan) => void
  onEdit: (plan: Plan) => void
  onWeather: (plan: Plan) => void
  onShare: (plan: Plan) => void
  onMembers: (plan: Plan) => void
  onChat: (plan: Plan) => void
  onDelete: (plan: Plan) => void
}

function statusLabel(status: Plan['estado']) {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export function PlanCard({
  plan,
  loadingPlan,
  loadingWeather,
  isFavorite,
  favoriteLoading,
  favoriteUnavailable,
  onToggleFavorite,
  onView,
  onEdit,
  onWeather,
  onShare,
  onMembers,
  onChat,
  onDelete,
}: PlanCardProps) {
  const owner = plan.ownership === 'OWNER'
  const member = plan.ownership === 'MEMBER'

  return (
    <article className="plan-card" data-testid="plan-card">
      <div className="plan-summary">
        <div className="plan-card-top">
          <span className="date-chip"><Icon name="calendar" size={14} />{plan.fecha}</span>
          <span className={`status-badge status-${plan.estado.toLowerCase()}`}>
            {statusLabel(plan.estado)}
          </span>
          <span className={`ownership-badge ownership-${plan.ownership.toLowerCase()}`}>
            {owner ? 'Propietario' : member ? 'Participante' : 'Vista administrativa'}
          </span>
        </div>
        <h3>{plan.titulo}</h3>
        <p className="plan-location">
          <Icon name="pin" size={15} />{plan.ciudad}<span aria-hidden="true">·</span>{plan.tipoActividad}
        </p>
      </div>
      <div className="card-actions">
        <button
          className="text-button plan-favorite-button"
          type="button"
          title={`${isFavorite ? 'Quitar de' : 'Agregar a'} favoritos: ${plan.titulo}`}
          aria-label={`${isFavorite ? 'Quitar de' : 'Agregar a'} favoritos: ${plan.titulo}`}
          aria-pressed={isFavorite}
          disabled={favoriteLoading || favoriteUnavailable}
          onClick={(event) => {
            event.stopPropagation()
            onToggleFavorite(plan)
          }}
        >
          <span aria-hidden="true">{isFavorite ? '★' : '☆'}</span>
          {favoriteLoading && 'Guardando...'}
        </button>
        <button className="text-button" onClick={() => onView(plan)} type="button" aria-label={`Ver ${plan.titulo}`}>
          <Icon name="eye" size={15} /> Ver
        </button>
        {owner && (
          <button
            className="text-button"
            onClick={() => onEdit(plan)}
            type="button"
            disabled={loadingPlan}
          >
            <Icon name="edit" size={15} /> {loadingPlan ? 'Cargando...' : 'Editar'}
          </button>
        )}
        <button
          className="text-button"
          onClick={() => onWeather(plan)}
          type="button"
          disabled={loadingWeather}
        >
          <Icon name="cloud" size={15} /> Clima
        </button>
        {(owner || member) && (
          <>
            <button className="text-button" onClick={() => onMembers(plan)} type="button">
              <Icon name="users" size={15} /> Participantes
            </button>
            <button className="text-button" onClick={() => onChat(plan)} type="button">
              <Icon name="chat" size={15} /> Chat
            </button>
          </>
        )}
        {owner && (
          <>
            <button className="text-button" onClick={() => onShare(plan)} type="button">
              <Icon name="share" size={15} /> Compartir
            </button>
            <button className="text-button danger-text" onClick={() => onDelete(plan)} type="button">
              <Icon name="trash" size={15} /> Eliminar
            </button>
          </>
        )}
      </div>
    </article>
  )
}
