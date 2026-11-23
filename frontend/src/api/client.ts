import axios from 'axios'
import { clearAccessToken, getAccessToken } from './session'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      getAccessToken()
    ) {
      clearAccessToken()
      window.dispatchEvent(new Event('climaplan:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export default api
