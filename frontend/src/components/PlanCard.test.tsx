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
  onView: () => {},
  onEdit: () => {},
  onWeather: () => {},
  onShare: () => {},
  onMembers: () => {},
  onDelete: () => {},
}

describe('PlanCard', () => {
  it('muestra etiqueta y acciones del propietario', () => {
    const markup = renderToStaticMarkup(<PlanCard {...callbacks} plan={plan} />)

    expect(markup).toContain('Propietario')
    expect(markup).toContain('Editar')
    expect(markup).toContain('Clima')
    expect(markup).toContain('Participantes')
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
    expect(markup).not.toContain('Editar')
    expect(markup).not.toContain('Compartir')
    expect(markup).not.toContain('Eliminar')
  })
})
