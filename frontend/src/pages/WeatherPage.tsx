import { useState, type FormEvent } from 'react'
import { weatherApi } from '../api/weather'
import { getApiErrorMessage } from '../api/errors'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import type { Weather } from '../types'

export function WeatherPage() {
  const [city, setCity] = useState('')
  const [weather, setWeather] = useState<Weather | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setWeather(null)
    try {
      setWeather(await weatherApi.current(city.trim()))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <span className="eyebrow">EL PRONÓSTICO DE TU DESTINO</span>
          <h1>Consulta el clima</h1>
          <p>Escribe una ciudad y revisa sus condiciones actuales antes de salir.</p>
        </div>
        <span className="heading-stamp stamp-sun" aria-hidden="true">☼</span>
      </div>

      <section className="weather-search-panel">
        <div className="weather-search-copy">
          <span className="eyebrow eyebrow-light">¿QUÉ TIEMPO HACE?</span>
          <h2>Elige tu próxima parada</h2>
          <p>Consulta temperatura, humedad y viento en un instante.</p>
        </div>
        <form className="weather-search-form" onSubmit={handleSubmit}>
          <label className="visually-hidden" htmlFor="weather-city">Ciudad</label>
          <input
            id="weather-city"
            name="city"
            value={city}
            onChange={(event) => setCity(event.target.value)}
            placeholder="Ej. Madrid"
            required
          />
          <button className="button button-light" disabled={loading}>
            {loading ? 'Buscando...' : 'Consultar'} <span aria-hidden="true">→</span>
          </button>
        </form>
      </section>

      {error && <ErrorMessage message={error} />}
      {loading && <LoadingState label="Buscando condiciones actuales..." />}

      {weather && (
        <article className="weather-result">
          <div className="weather-result-main">
            <span className="weather-sun" aria-hidden="true">☀</span>
            <div>
              <span className="eyebrow">CLIMA ACTUAL</span>
              <h2>{weather.ciudad}, {weather.pais}</h2>
              <p>{weather.descripcion}</p>
            </div>
            <strong className="temperature">{Math.round(weather.temperatura)}°</strong>
          </div>
          <div className="weather-metrics">
            <div><span>Sensación térmica</span><strong>{weather.sensacionTermica}°C</strong></div>
            <div><span>Humedad</span><strong>{weather.humedad}%</strong></div>
            <div><span>Viento</span><strong>{weather.velocidadViento} m/s</strong></div>
            <div><span>Coordenadas</span><strong>{weather.lat}, {weather.lon}</strong></div>
          </div>
        </article>
      )}
    </section>
  )
}
