import api from './client'
import type { AuthLoginResponse, AuthResponse } from '../types'

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput extends LoginInput {
  nombre: string
}

export interface AuthOperationResponse {
  message: string
}

export const authApi = {
  async login(input: LoginInput): Promise<AuthLoginResponse> {
    const response = await api.post<AuthLoginResponse>('/api/v1/auth/login', input)
    return response.data
  },

  async register(input: RegisterInput): Promise<AuthOperationResponse> {
    const response = await api.post<AuthOperationResponse>('/api/v1/auth/register', input)
    return response.data
  },

  async verifyEmail(input: { email: string; code: string }): Promise<AuthOperationResponse> {
    const response = await api.post<AuthOperationResponse>('/api/v1/auth/verify-email', input)
    return response.data
  },

  async resendVerification(input: { email: string }): Promise<AuthOperationResponse> {
    const response = await api.post<AuthOperationResponse>('/api/v1/auth/resend-verification', input)
    return response.data
  },

  async verifyTwoFactor(input: { challengeId: string; code: string }): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/api/v1/auth/verify-2fa', input)
    return response.data
  },
}
