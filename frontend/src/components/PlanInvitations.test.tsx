import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PlanInvitations } from './PlanInvitations'
import type { PlanInvitation } from '../types'

const invitation: PlanInvitation = {
  id: 27,
  planId: 91,
  tituloPlan: 'Viaje a San Cristóbal',
  ciudad: 'San Cristóbal',
  propietarioNombre: 'Gustavo',
  invitedAt: '2026-11-24T09:00:00',
}

describe('PlanInvitations', () => {
  it('muestra el plan invitado y las acciones para resolver la invitación', () => {
    const markup = renderToStaticMarkup(
      <PlanInvitations
        invitations={[invitation]}
        loading={false}
        error=""
        actionId={null}
        onRefresh={() => {}}
        onAccept={() => {}}
        onReject={() => {}}
      />,
    )

    expect(markup).toContain('Invitaciones pendientes')
    expect(markup).toContain('Gustavo te invitó a')
    expect(markup).toContain('Viaje a San Cristóbal')
    expect(markup).toContain('Aceptar')
    expect(markup).toContain('Rechazar')
  })

  it('muestra el estado vacío cuando no hay invitaciones', () => {
    const markup = renderToStaticMarkup(
      <PlanInvitations
        invitations={[]}
        loading={false}
        error=""
        actionId={null}
        onRefresh={() => {}}
        onAccept={() => {}}
        onReject={() => {}}
      />,
    )

    expect(markup).toContain('No tienes invitaciones pendientes.')
  })
})
