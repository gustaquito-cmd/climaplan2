import { describe, expect, it, vi } from 'vitest'
import type { AuthResponse } from '../types'
import { getLoginFlowResult, verifyTwoFactorAndAuthenticate } from './authFlow'

const authResponse: AuthResponse = {
  accessToken: 'jwt-token',
  tokenType: 'Bearer',
  expiresIn: 3_600_000,
  usuario: {
    id: 8,
    nombre: 'Gustavo',
    email: 'gustavo@example.com',
    rol: 'ROLE_USER',
    estado: 'ACTIVO',
  },
}

describe('authFlow', () => {
  it('keeps the normal login flow when the backend returns a JWT', () => {
    expect(getLoginFlowResult(authResponse)).toEqual({
      kind: 'authenticated',
      response: authResponse,
    })
  })

  it('routes a two-factor login to its challenge', () => {
    expect(getLoginFlowResult({
      twoFactorRequired: true,
      challengeId: 'challenge-uuid',
      message: 'Código enviado',
    })).toEqual({
      kind: 'two-factor-required',
      challengeId: 'challenge-uuid',
    })
  })

  it('stores the returned authentication response only after OTP verification', async () => {
    const verify = vi.fn().mockResolvedValue(authResponse)
    const setAuthenticated = vi.fn()

    await verifyTwoFactorAndAuthenticate('challenge-uuid', '123456', verify, setAuthenticated)

    expect(verify).toHaveBeenCalledWith({ challengeId: 'challenge-uuid', code: '123456' })
    expect(setAuthenticated).toHaveBeenCalledWith(authResponse)
  })

  it('does not authenticate if OTP verification fails', async () => {
    const verify = vi.fn().mockRejectedValue(new Error('OTP inválido'))
    const setAuthenticated = vi.fn()

    await expect(
      verifyTwoFactorAndAuthenticate('challenge-uuid', '000000', verify, setAuthenticated),
    ).rejects.toThrow('OTP inválido')
    expect(setAuthenticated).not.toHaveBeenCalled()
  })
})
