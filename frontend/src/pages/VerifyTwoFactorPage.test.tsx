import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { VerifyTwoFactorPage } from './VerifyTwoFactorPage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ setAuthenticated: vi.fn() }),
}))

describe('VerifyTwoFactorPage', () => {
  it('renders the OTP form for the login challenge without displaying its ID', () => {
    const markup = renderToStaticMarkup(
      <MemoryRouter initialEntries={[{
        pathname: '/verify-2fa',
        state: { challengeId: 'challenge-uuid' },
      }]}>
        <Routes>
          <Route path="/verify-2fa" element={<VerifyTwoFactorPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(markup).toContain('Código de seis dígitos')
    expect(markup).toContain('name="code"')
    expect(markup).toContain('Verificar')
    expect(markup).not.toContain('challenge-uuid')
  })
})
