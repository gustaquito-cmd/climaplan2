import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PlanCard } from './PlanCard'
import type { Plan } from '../types'

const plan: Plan = {
  id: 1,
  titulo: 'Viaje a San Cristóbal',
  ciudad: 'San Cristóbal',
  fecha: '2026-12-01',
  tipoActividad: 'Viaje',
  notas: '',
  estado: 'PENDIENTE',
  usuarioId: 7,
  nombreUsuario: 'Gustavo',
  ownership: 'OWNER',
}

const callbacks = {
  loadingPlan: false,
  loadingWeather: false,
  isFavorite: false,
  favoriteLoading: false,
  favoriteUnavailable: false,
  onToggleFavorite: () => {},
  onView: () => {},
  onEdit: () => {},
  onWeather: () => {},
  onShare: () => {},
  onMembers: () => {},
  onChat: () => {},
  onDelete: () => {},
}

describe('PlanCard', () => {
  it('muestra etiqueta y acciones del propietario', () => {
    const markup = renderToStaticMarkup(<PlanCard {...callbacks} plan={plan} />)

    expect(markup).toContain('Propietario')
    expect(markup).toContain('Editar')
    expect(markup).toContain('Clima')
    expect(markup).toContain('Participantes')
    expect(markup).toContain('Chat')
    expect(markup).toContain('Compartir')
    expect(markup).toContain('Eliminar')
  })

  it('muestra solo lectura, clima y participantes para un miembro', () => {
    const markup = renderToStaticMarkup(
      <PlanCard {...callbacks} plan={{ ...plan, ownership: 'MEMBER' }} />,
    )

    expect(markup).toContain('Participante')
    expect(markup).toContain('Ver')
    expect(markup).toContain('Clima')
    expect(markup).toContain('Participantes')
    expect(markup).toContain('Chat')
    expect(markup).not.toContain('Editar')
    expect(markup).not.toContain('Compartir')
    expect(markup).not.toContain('Eliminar')
  })

  it('muestra estado accesible y deshabilita la estrella durante la solicitud', () => {
    const unfavorited = renderToStaticMarkup(<PlanCard {...callbacks} plan={plan} />)
    expect(unfavorited).toContain('☆')
    expect(unfavorited).toContain('aria-pressed="false"')
    expect(unfavorited).toContain('Agregar a favoritos: Viaje a San Cristóbal')

    const pending = renderToStaticMarkup(
      <PlanCard {...callbacks} plan={plan} isFavorite favoriteLoading />,
    )
    expect(pending).toContain('★')
    expect(pending).toContain('aria-pressed="true"')
    expect(pending).toContain('Quitar de favoritos: Viaje a San Cristóbal')
    expect(pending).toContain('disabled=""')
    expect(pending).toContain('Guardando...')
  })
})
