import api from './client'
import type {
  Plan,
  PlanInput,
  PlanShareStatus,
  PlanWeatherCheck,
  SharedPlan,
} from '../types'
import { publicApi } from './client'

export const plansApi = {
  async list(): Promise<Plan[]> {
    const response = await api.get<Plan[]>('/api/v1/plans')
    return response.data
  },

  async get(id: number): Promise<Plan> {
    const response = await api.get<Plan>(`/api/v1/plans/${id}`)
    return response.data
  },

  async create(input: PlanInput): Promise<Plan> {
    const response = await api.post<Plan>('/api/v1/plans', input)
    return response.data
  },

  async update(id: number, input: PlanInput): Promise<Plan> {
    const response = await api.put<Plan>(`/api/v1/plans/${id}`, input)
    return response.data
  },

  async remove(id: number): Promise<void> {
    await api.delete(`/api/v1/plans/${id}`)
  },

  async weatherCheck(id: number): Promise<PlanWeatherCheck> {
    const response = await api.get<PlanWeatherCheck>(
      `/api/v1/plans/${id}/weather-check`,
    )
    return response.data
  },

  async createShare(id: number): Promise<PlanShareStatus> {
    const response = await api.post<PlanShareStatus>(
      `/api/v1/plans/${id}/share`,
    )
    return response.data
  },

  async getShare(id: number): Promise<PlanShareStatus> {
    const response = await api.get<PlanShareStatus>(
      `/api/v1/plans/${id}/share`,
    )
    return response.data
  },

  async revokeShare(id: number): Promise<void> {
    await api.delete(`/api/v1/plans/${id}/share`)
  },

  async getSharedPlan(token: string): Promise<SharedPlan> {
    const response = await publicApi.get<SharedPlan>(
      `/api/v1/shared/plans/${encodeURIComponent(token)}`,
    )
    return response.data
  },
}
