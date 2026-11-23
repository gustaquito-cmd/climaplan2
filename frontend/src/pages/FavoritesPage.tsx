import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { favoritesApi } from '../api/favorites'
import { getApiErrorMessage } from '../api/errors'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import type { Favorite, FavoriteInput } from '../types'

const emptyFavorite = (): FavoriteInput => ({
  ciudad: '',
  pais: '',
  latitud: 0,
  longitud: 0,
})

export function FavoritesPage() {
  const [favorites, setFavorites] = useState<Favorite[]>([])
  const [form, setForm] = useState<FavoriteInput>(emptyFavorite)
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')

  const loadFavorites = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      setFavorites(await favoritesApi.list())
    } catch (requestError) {
      setLoadError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadFavorites()
  }, [loadFavorites])

  function resetForm() {
    setForm(emptyFavorite())
    setLatitude('')
    setLongitude('')
    setEditingId(null)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const latitud = Number(latitude)
    const longitud = Number(longitude)
    if (!Number.isFinite(latitud) || !Number.isFinite(longitud)) {
      setError('Ingresa una latitud y longitud válidas.')
      return
    }

    setSaving(true)
    setError('')
    try {
      const input = { ...form, latitud, longitud }
      if (editingId === null) {
        await favoritesApi.create(input)
      } else {
        await favoritesApi.update(editingId, input)
      }
      resetForm()
      await loadFavorites()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function beginEdit(id: number) {
    setError('')
    try {
      const favorite = await favoritesApi.get(id)
      setForm({
        ciudad: favorite.ciudad,
        pais: favorite.pais,
        latitud: favorite.latitud,
        longitud: favorite.longitud,
      })
      setLatitude(String(favorite.latitud))
      setLongitude(String(favorite.longitud))
      setEditingId(favorite.id)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  async function removeFavorite(favorite: Favorite) {
    if (!window.confirm(`¿Eliminar ${favorite.ciudad} de tus favoritos?`)) return
    setError('')
    try {
      await favoritesApi.remove(favorite.id)
      await loadFavorites()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <span className="eyebrow">LUGARES QUE TE INSPIRAN</span>
          <h1>Destinos favoritos</h1>
          <p>Guarda coordenadas para volver a encontrar tus lugares especiales.</p>
        </div>
        <span className="heading-stamp stamp-heart" aria-hidden="true">♡</span>
      </div>

      <div className="content-grid favorites-grid">
        <section className="panel form-panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">{editingId === null ? 'NUEVO DESTINO' : 'EDITAR DESTINO'}</span>
              <h2>{editingId === null ? 'Guarda un lugar' : 'Actualiza el lugar'}</h2>
            </div>
            <span className="panel-icon" aria-hidden="true">⌖</span>
          </div>
          {error && <ErrorMessage message={error} />}
          <form className="form-stack" onSubmit={handleSubmit}>
            <label>
              Ciudad
              <input
                value={form.ciudad}
                onChange={(event) => setForm((current) => ({ ...current, ciudad: event.target.value }))}
                placeholder="Ej. Oaxaca"
                maxLength={100}
                required
              />
            </label>
            <label>
              País
              <input
                value={form.pais}
                onChange={(event) => setForm((current) => ({ ...current, pais: event.target.value }))}
                placeholder="Ej. México"
                maxLength={100}
                required
              />
            </label>
            <div className="form-row">
              <label>
                Latitud
                <input
                  type="number"
                  min="-90"
                  max="90"
                  step="any"
                  value={latitude}
                  onChange={(event) => setLatitude(event.target.value)}
                  placeholder="16.7538"
                  required
                />
              </label>
              <label>
                Longitud
                <input
                  type="number"
                  min="-180"
                  max="180"
                  step="any"
                  value={longitude}
                  onChange={(event) => setLongitude(event.target.value)}
                  placeholder="-93.116"
                  required
                />
              </label>
            </div>
            <p className="field-hint">Puedes obtener coordenadas desde el mapa de tu preferencia.</p>
            <div className="form-actions">
              <button className="button button-primary" disabled={saving}>
                {saving ? 'Guardando...' : editingId === null ? 'Guardar destino' : 'Guardar cambios'}
              </button>
              {editingId !== null && (
                <button className="button button-secondary" type="button" onClick={resetForm}>
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="plans-column">
          <div className="list-heading">
            <div>
              <span className="eyebrow">TU COLECCIÓN</span>
              <h2>Lugares guardados <span className="count-badge">{favorites.length}</span></h2>
            </div>
            <button className="button button-quiet" onClick={() => void loadFavorites()} type="button">
              Actualizar
            </button>
          </div>
          {loadError && <ErrorMessage message={loadError} />}
          {loading ? (
            <LoadingState label="Cargando tus destinos..." />
          ) : favorites.length === 0 ? (
            <div className="empty-state">
              <span aria-hidden="true">♡</span>
              <h3>Tu mapa está por comenzar</h3>
              <p>Agrega tu primer destino favorito en el formulario.</p>
            </div>
          ) : (
            <div className="favorite-grid">
              {favorites.map((favorite) => (
                <article className="favorite-card" key={favorite.id}>
                  <div className="favorite-map-mark" aria-hidden="true">⌖</div>
                  <span className="eyebrow">DESTINO FAVORITO</span>
                  <h3>{favorite.ciudad}</h3>
                  <p>{favorite.pais}</p>
                  <div className="coordinate-line">
                    <span>{favorite.latitud}</span>
                    <span>{favorite.longitud}</span>
                  </div>
                  <div className="card-actions">
                    <button className="text-button" onClick={() => void beginEdit(favorite.id)} type="button">
                      Editar
                    </button>
                    <button className="text-button danger-text" onClick={() => void removeFavorite(favorite)} type="button">
                      Eliminar
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </section>
  )
}
