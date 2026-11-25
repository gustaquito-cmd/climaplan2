export type Role = 'ROLE_USER' | 'ROLE_ADMIN'
export type PlanStatus = 'PENDIENTE' | 'COMPLETADO' | 'CANCELADO'

export interface Usuario {
  id: number
  nombre: string
  email: string
  rol: Role
  estado: 'ACTIVO' | 'INACTIVO'
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  usuario: Usuario
}

export interface ApiErrorResponse {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
  validationErrors: Record<string, string> | null
}

export interface PlanInput {
  titulo: string
  ciudad: string
  fecha: string
  tipoActividad: string
  notas: string
  estado: PlanStatus
}

export interface Plan extends PlanInput {
  id: number
  usuarioId: number
  nombreUsuario: string
}

export interface PlanShareStatus {
  token: string | null
  active: boolean
  createdAt: string | null
  expiresAt: string | null
}

export interface SharedPlan {
  titulo: string
  ciudad: string
  fecha: string
  tipoActividad: string
  estado: PlanStatus
  estadoClimatico: 'FAVORABLE' | 'PRECAUCION' | 'NO_RECOMENDABLE' | null
  clima: SharedWeather | null
  recomendacion: string | null
  avisoClimatico: string | null
}

export interface SharedWeather {
  temperatura: number
  sensacionTermica: number
  humedad: number
  descripcion: string
  velocidadViento: number
}

export interface FavoriteInput {
  ciudad: string
  pais: string
  latitud: number
  longitud: number
}

export interface Favorite extends FavoriteInput {
  id: number
  usuarioId: number
}

export interface Weather {
  ciudad: string
  pais: string
  lat: number
  lon: number
  temperatura: number
  sensacionTermica: number
  humedad: number
  descripcion: string
  velocidadViento: number
}

export interface PlanWeatherCheck {
  planId: number
  titulo: string
  ciudad: string
  fecha: string
  estadoClimatico: 'FAVORABLE' | 'PRECAUCION' | 'NO_RECOMENDABLE'
  clima: Weather
  recomendacion: string
}
