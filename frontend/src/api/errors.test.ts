import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from './errors'
import {
  clearAccessToken,
  getAccessToken,
  storeAccessToken,
} from './session'

describe('getApiErrorMessage', () => {
  it('uses the backend conflict message', () => {
    expect(
      getApiErrorMessage({
        response: {
          status: 409,
          data: { message: 'El email ya está registrado' },
        },
      }),
    ).toBe('El email ya está registrado')
  })

  it('includes backend validation details', () => {
    expect(
      getApiErrorMessage({
        response: {
          status: 400,
          data: {
            message: 'La solicitud contiene campos inválidos',
            validationErrors: { email: 'El email debe ser válido' },
          },
        },
      }),
    ).toBe(
      'La solicitud contiene campos inválidos email: El email debe ser válido',
    )
  })

  it('provides a useful message when the server cannot be reached', () => {
    expect(getApiErrorMessage(new Error('Network Error'))).toContain(
      'No se pudo conectar',
    )
  })
})

describe('access token storage', () => {
  it('stores, reads, and removes only the access token', () => {
    const values = new Map<string, string>()
    const storage: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> = {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: (key) => values.delete(key),
    }

    storeAccessToken('access-token', storage)
    expect(getAccessToken(storage)).toBe('access-token')

    clearAccessToken(storage)
    expect(getAccessToken(storage)).toBeNull()
    expect([...values.keys()]).toEqual([])
  })
})
