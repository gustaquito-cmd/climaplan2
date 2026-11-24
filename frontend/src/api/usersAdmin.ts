import api from './client'
import type { Usuario } from '../types'

export type UsuarioAdminUpdate = Pick<Usuario, 'nombre' | 'email'>

export const usersAdminApi = {
  async list(): Promise<Usuario[]> {
    const response = await api.get<Usuario[]>('/api/v1/users')
    return response.data
  },

  async get(id: number): Promise<Usuario> {
    const response = await api.get<Usuario>(`/api/v1/users/${id}`)
    return response.data
  },

  async update(id: number, input: UsuarioAdminUpdate): Promise<Usuario> {
    const response = await api.put<Usuario>(`/api/v1/users/${id}`, input)
    return response.data
  },

  async deactivate(id: number): Promise<void> {
    await api.delete(`/api/v1/users/${id}`)
  },
}
