import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PlanStatusField } from './PlanStatusField'

describe('PlanStatusField', () => {
  it('does not render a status selector during plan creation', () => {
    const markup = renderToStaticMarkup(
      <PlanStatusField editing={false} value="PENDIENTE" onChange={() => {}} />,
    )

    expect(markup).toBe('')
  })

  it('renders the existing status options during plan editing', () => {
    const markup = renderToStaticMarkup(
      <PlanStatusField editing value="PENDIENTE" onChange={() => {}} />,
    )

    expect(markup).toContain('<select')
    expect(markup).toContain('Completado')
    expect(markup).toContain('Cancelado')
  })
})
