export type Role = 'ROLE_USER' | 'ROLE_ADMIN'
export type PlanStatus = 'PENDIENTE' | 'COMPLETADO' | 'CANCELADO'
export type PlanOwnership = 'OWNER' | 'MEMBER' | 'ADMIN'
export type PlanMemberStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'
export type WeatherDataType =
  | 'CURRENT'
  | 'FORECAST'
  | 'NOT_AVAILABLE'
  | 'PAST_DATE'
  | 'SERVICE_ERROR'

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

export type PlanCreateInput = Omit<PlanInput, 'estado'>

export interface Plan extends PlanInput {
  id: number
  usuarioId: number
  nombreUsuario: string
  ownership: PlanOwnership
}

export interface PlanMember {
  id: number
  nombre: string
  email: string
  estado: PlanMemberStatus
  rol: Exclude<PlanOwnership, 'ADMIN'>
}

export interface PlanInvitation {
  id: number
  planId: number
  tituloPlan: string
  ciudad: string
  propietarioNombre: string
  invitedAt: string
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
  weatherDataType: WeatherDataType
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
  weatherDataType: WeatherDataType
  estadoClimatico: 'FAVORABLE' | 'PRECAUCION' | 'NO_RECOMENDABLE' | null
  clima: Weather | null
  recomendacion: string | null
  mensaje: string
}
