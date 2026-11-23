import api from './client'
import type { AuthResponse } from '../types'

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput extends LoginInput {
  nombre: string
}

export const authApi = {
  async login(input: LoginInput): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/api/v1/auth/login', input)
    return response.data
  },

  async register(input: RegisterInput): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/api/v1/auth/register', input)
    return response.data
  },
}
