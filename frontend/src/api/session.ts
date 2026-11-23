export const TOKEN_KEY = 'climaplan_access_token'

type TokenStorage = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>

export function getAccessToken(storage: TokenStorage = localStorage): string | null {
  return storage.getItem(TOKEN_KEY)
}

export function storeAccessToken(
  token: string,
  storage: TokenStorage = localStorage,
): void {
  storage.setItem(TOKEN_KEY, token)
}

export function clearAccessToken(storage: TokenStorage = localStorage): void {
  storage.removeItem(TOKEN_KEY)
}
