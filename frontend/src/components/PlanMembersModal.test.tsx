import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PlanMembersModal } from './PlanMembersModal'
import type { Plan, PlanMember } from '../types'

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

const members: PlanMember[] = [
  { id: 7, nombre: 'Gustavo', email: 'gustavo@example.com', estado: 'ACCEPTED', rol: 'OWNER' },
  { id: 9, nombre: 'Carlos', email: 'carlos@example.com', estado: 'ACCEPTED', rol: 'MEMBER' },
]

const baseProps = {
  plan,
  members,
  loading: false,
  email: '',
  inviting: false,
  error: '',
  removingUserId: null,
  onEmailChange: () => {},
  onInvite: () => {},
  onRemove: () => {},
  onClose: () => {},
}

describe('PlanMembersModal', () => {
  it('lets the owner invite and remove an accepted participant', () => {
    const markup = renderToStaticMarkup(
      <PlanMembersModal {...baseProps} owner />,
    )

    expect(markup).toContain('Propietario')
    expect(markup).toContain('Participantes')
    expect(markup).toContain('Enviar invitación')
    expect(markup).toContain('Eliminar')
  })

  it('does not render owner management controls for a participant', () => {
    const markup = renderToStaticMarkup(
      <PlanMembersModal {...baseProps} owner={false} />,
    )

    expect(markup).not.toContain('Enviar invitación')
    expect(markup).not.toContain('Eliminar')
  })
})
