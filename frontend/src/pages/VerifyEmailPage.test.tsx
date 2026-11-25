import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { VerifyEmailPage } from './VerifyEmailPage'

describe('VerifyEmailPage', () => {
  it('muestra el formulario para ingresar y reenviar el código', () => {
    const markup = renderToStaticMarkup(
      <MemoryRouter initialEntries={[{ pathname: '/verify-email', state: { email: 'user@example.com' } }]}>
        <Routes>
          <Route path="/verify-email" element={<VerifyEmailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(markup).toContain('VERIFICACIÓN DE CORREO')
    expect(markup).toContain('name="email"')
    expect(markup).toContain('value="user@example.com"')
    expect(markup).toContain('name="code"')
    expect(markup).toContain('Verificar correo')
    expect(markup).toContain('Reenviar código')
  })
})
