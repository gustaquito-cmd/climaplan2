import { describe, expect, it } from 'vitest'
import { weatherDataLabel } from '../utils/weatherDisplay'

describe('weatherDataLabel', () => {
  it('distinguishes current weather and a dated forecast', () => {
    expect(weatherDataLabel('CURRENT', '2026-11-24')).toBe('Clima actual')
    expect(weatherDataLabel('FORECAST', '2026-11-28')).toBe(
      'Pronóstico para 28 de noviembre de 2026',
    )
  })

  it('explains unavailable, past-date, and provider-error states', () => {
    expect(weatherDataLabel('NOT_AVAILABLE', '2026-12-10')).toBe(
      'Pronóstico todavía no disponible',
    )
    expect(weatherDataLabel('PAST_DATE', '2026-11-20')).toBe(
      'La fecha de este plan ya pasó',
    )
    expect(weatherDataLabel('SERVICE_ERROR', '2026-11-28')).toBe(
      'Clima no disponible en este momento',
    )
  })
})
