import api from './client'
import type { Favorite, FavoriteInput } from '../types'

export const favoritesApi = {
  async list(): Promise<Favorite[]> {
    const response = await api.get<Favorite[]>('/api/v1/favorites')
    return response.data
  },

  async get(id: number): Promise<Favorite> {
    const response = await api.get<Favorite>(`/api/v1/favorites/${id}`)
    return response.data
  },

  async create(input: FavoriteInput): Promise<Favorite> {
    const response = await api.post<Favorite>('/api/v1/favorites', input)
    return response.data
  },

  async update(id: number, input: FavoriteInput): Promise<Favorite> {
    const response = await api.put<Favorite>(`/api/v1/favorites/${id}`, input)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/api/v1/favorites/${id}`)
  },
}
