import type { AuthResponse, AuthLoginResponse } from '../types'

export type LoginFlowResult =
  | { kind: 'authenticated'; response: AuthResponse }
  | { kind: 'two-factor-required'; challengeId: string }

export function getLoginFlowResult(response: AuthLoginResponse): LoginFlowResult {
  if ('accessToken' in response) {
    return { kind: 'authenticated', response }
  }
  return { kind: 'two-factor-required', challengeId: response.challengeId }
}

export async function verifyTwoFactorAndAuthenticate(
  challengeId: string,
  code: string,
  verify: (input: { challengeId: string; code: string }) => Promise<AuthResponse>,
  setAuthenticated: (response: AuthResponse) => void,
): Promise<void> {
  const response = await verify({ challengeId, code })
  setAuthenticated(response)
}
