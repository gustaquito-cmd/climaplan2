import api from './client'
import type { Usuario } from '../types'

export const usersApi = {
  async me(): Promise<Usuario> {
    const response = await api.get<Usuario>('/api/v1/users/me')
    return response.data
  },
}
