import { useState, type FormEvent } from 'react'
import { weatherApi } from '../api/weather'
import { getApiErrorMessage } from '../api/errors'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { PageHeader } from '../components/PageHeader'
import type { Weather } from '../types'

export function WeatherPage() {
  const [city, setCity] = useState('')
  const [weather, setWeather] = useState<Weather | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const query = city.trim()
    if (!query) return
    setLoading(true)
    setError('')
    setWeather(null)
    try {
      setWeather(await weatherApi.current(query))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <PageHeader
        title="Consultar clima"
        description="Busca las condiciones actuales de una ciudad."
      />

      <form className="weather-search-form" onSubmit={handleSubmit}>
        <label className="visually-hidden" htmlFor="weather-city">Ciudad</label>
        <Icon name="pin" size={18} />
        <input
          id="weather-city"
          name="city"
          value={city}
          onChange={(event) => setCity(event.target.value)}
          placeholder="Nombre de la ciudad"
          autoComplete="address-level2"
          required
        />
        <button className="button button-primary" disabled={loading}>
          {loading ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {error && <ErrorMessage message={error} />}
      {loading && <LoadingState label="Consultando condiciones actuales..." />}

      {weather && (
        <article className="weather-result" aria-label={`Clima en ${weather.ciudad}`}>
          <div className="weather-result-main">
            <span className="weather-symbol"><Icon name="cloud" size={23} /></span>
            <div className="weather-location">
              <span className="eyebrow">CONDICIONES ACTUALES</span>
              <h2>{weather.ciudad}, {weather.pais}</h2>
              <p>{weather.descripcion}</p>
            </div>
            <strong className="temperature">{Math.round(weather.temperatura)}<span>°C</span></strong>
          </div>
          <dl className="weather-metrics">
            <div><dt>Sensación térmica</dt><dd>{weather.sensacionTermica}°C</dd></div>
            <div><dt>Humedad</dt><dd>{weather.humedad}%</dd></div>
            <div><dt>Viento</dt><dd>{weather.velocidadViento} m/s</dd></div>
          </dl>
        </article>
      )}
    </section>
  )
}
