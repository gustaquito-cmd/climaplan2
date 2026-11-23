import api from './client'
import type { Weather } from '../types'

export const weatherApi = {
  async current(city: string): Promise<Weather> {
    const response = await api.get<Weather>('/api/v1/weather', {
      params: { city },
    })
    return response.data
  },
}
