import type { WeatherDataType } from '../types'

export function weatherDataLabel(type: WeatherDataType, date: string): string {
  switch (type) {
    case 'CURRENT':
      return 'Clima actual'
    case 'FORECAST':
      return `Pronóstico para ${formatDate(date)}`
    case 'NOT_AVAILABLE':
      return 'Pronóstico todavía no disponible'
    case 'PAST_DATE':
      return 'La fecha de este plan ya pasó'
    case 'SERVICE_ERROR':
      return 'Clima no disponible en este momento'
  }
}

function formatDate(date: string): string {
  return new Intl.DateTimeFormat('es-MX', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(`${date}T00:00:00Z`))
}
