import type { SharedWeather, Weather, WeatherDataType } from '../types'
import { weatherDataLabel } from '../utils/weatherDisplay'

export interface WeatherResultProps {
  weatherDataType: WeatherDataType
  fecha: string
  ciudad: string
  estadoClimatico: 'FAVORABLE' | 'PRECAUCION' | 'NO_RECOMENDABLE' | null
  clima: Weather | SharedWeather | null
  recomendacion: string | null
  mensaje: string | null
  publicView?: boolean
}

export function WeatherResult({
  weatherDataType,
  fecha,
  ciudad,
  estadoClimatico,
  clima,
  recomendacion,
  mensaje,
  publicView = false,
}: WeatherResultProps) {
  return (
    <div className="weather-check-detail">
      <div className="weather-check-top">
        <div>
          <span className="muted">{fecha}</span>
          {publicView
            ? <h2 id="shared-weather-heading">{ciudad}</h2>
            : <h3>{ciudad}</h3>}
        </div>
        {estadoClimatico && (
          <span className={`weather-status weather-${estadoClimatico.toLowerCase()}`}>
            {estadoClimatico.replaceAll('_', ' ').toLowerCase()}
          </span>
        )}
      </div>
      <p className="eyebrow">{weatherDataLabel(weatherDataType, fecha)}</p>
      {clima ? (
        <>
          <p className="weather-description">{clima.descripcion}</p>
          <div className="weather-metrics">
            <div><span>Temperatura</span><strong>{clima.temperatura}°C</strong></div>
            {publicView && 'sensacionTermica' in clima && (
              <div><span>Sensación térmica</span><strong>{clima.sensacionTermica}°C</strong></div>
            )}
            <div><span>Humedad</span><strong>{clima.humedad}%</strong></div>
            <div><span>Viento</span><strong>{clima.velocidadViento} m/s</strong></div>
          </div>
          {recomendacion && (
            <div className="recommendation">
              <strong>Recomendación</strong>
              <p>{recomendacion}</p>
            </div>
          )}
        </>
      ) : (
        <div className="feedback feedback-info" role="status">
          <p>{mensaje ?? weatherDataLabel(weatherDataType, fecha)}</p>
        </div>
      )}
    </div>
  )
}
