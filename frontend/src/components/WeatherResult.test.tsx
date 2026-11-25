import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { WeatherResultProps } from './WeatherResult'
import { WeatherResult } from './WeatherResult'

const baseProps: WeatherResultProps = {
  weatherDataType: 'FORECAST',
  fecha: '2026-11-28',
  ciudad: 'Tuxtla Gutiérrez',
  estadoClimatico: 'FAVORABLE',
  clima: {
    temperatura: 24,
    sensacionTermica: 25,
    humedad: 50,
    descripcion: 'cielo despejado',
    velocidadViento: 3,
  },
  recomendacion: 'Condiciones pronosticadas favorables',
  mensaje: null,
}

describe('WeatherResult', () => {
  it('renders current weather and available metrics', () => {
    const markup = renderToStaticMarkup(
      <WeatherResult {...baseProps} weatherDataType="CURRENT" />,
    )

    expect(markup).toContain('Clima actual')
    expect(markup).toContain('24°C')
  })

  it('renders a dated forecast and its metrics for the shared-plan view', () => {
    const markup = renderToStaticMarkup(<WeatherResult {...baseProps} publicView />)

    expect(markup).toContain('Pronóstico para 28 de noviembre de 2026')
    expect(markup).toContain('24°C')
    expect(markup).toContain('cielo despejado')
    expect(markup).toContain('Sensación térmica')
  })

  it.each([
    ['NOT_AVAILABLE', 'Pronóstico todavía no disponible'],
    ['PAST_DATE', 'La fecha de este plan ya pasó'],
  ] as const)('shows the %s state without fabricated temperature data', (type, label) => {
    const markup = renderToStaticMarkup(
      <WeatherResult
        {...baseProps}
        weatherDataType={type}
        estadoClimatico={null}
        clima={null}
        recomendacion={null}
        mensaje={label}
        publicView
      />,
    )

    expect(markup).toContain(label)
    expect(markup).not.toContain('Temperatura')
    expect(markup).not.toContain('24°C')
  })
})
