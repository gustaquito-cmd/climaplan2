import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { favoritesApi } from '../api/favorites'
import { ConfirmModal } from '../components/ConfirmModal'
import { EmptyState, ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../context/ToastContext'
import type { Favorite, FavoriteInput } from '../types'

const emptyFavorite = (): FavoriteInput => ({
  ciudad: '',
  pais: '',
  latitud: 0,
  longitud: 0,
})

export function FavoritesPage() {
  const { showToast } = useToast()
  const [favorites, setFavorites] = useState<Favorite[]>([])
  const [form, setForm] = useState<FavoriteInput>(emptyFavorite)
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [removing, setRemoving] = useState(false)
  const [loadingFavoriteId, setLoadingFavoriteId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [deleteFavorite, setDeleteFavorite] = useState<Favorite | null>(null)

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

  function openCreate() {
    resetForm()
    setError('')
    setFormOpen(true)
  }

  function closeForm() {
    if (saving) return
    setFormOpen(false)
    setError('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const latitud = Number(latitude)
    const longitud = Number(longitude)
    if (
      !Number.isFinite(latitud) ||
      !Number.isFinite(longitud) ||
      latitud < -90 ||
      latitud > 90 ||
      longitud < -180 ||
      longitud > 180
    ) {
      setError('Ingresa una latitud entre -90 y 90 y una longitud entre -180 y 180.')
      return
    }

    setSaving(true)
    setError('')
    try {
      const updating = editingId !== null
      const input = { ...form, latitud, longitud }
      if (editingId === null) {
        await favoritesApi.create(input)
      } else {
        await favoritesApi.update(editingId, input)
      }
      setFormOpen(false)
      resetForm()
      showToast(updating ? 'Favorito guardado correctamente.' : 'Favorito agregado correctamente.')
      await loadFavorites()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function beginEdit(id: number) {
    setError('')
    setLoadingFavoriteId(id)
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
      setFormOpen(true)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoadingFavoriteId(null)
    }
  }

  async function confirmRemove() {
    if (!deleteFavorite) return
    setRemoving(true)
    setError('')
    try {
      await favoritesApi.remove(deleteFavorite.id)
      setDeleteFavorite(null)
      showToast('Favorito eliminado correctamente.')
      await loadFavorites()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
      setDeleteFavorite(null)
    } finally {
      setRemoving(false)
    }
  }

  return (
    <section>
      <PageHeader
        title="Favoritos"
        description="Guarda destinos y sus coordenadas para consultarlos cuando quieras."
        action={
          <button className="button button-primary" type="button" onClick={openCreate}>
            <Icon name="plus" size={17} /> Agregar destino
          </button>
        }
      />

      {error && <ErrorMessage message={error} />}
      {loadError && <ErrorMessage message={loadError} />}
      {loading ? (
        <LoadingState label="Cargando favoritos..." />
      ) : favorites.length === 0 ? (
        <EmptyState
          title="Aún no tienes destinos favoritos"
          description="Agrega una ciudad para tenerla a mano."
        />
      ) : (
        <section className="data-section" aria-label="Destinos favoritos">
          <div className="list-heading">
            <h2>Destinos guardados <span className="count-badge">{favorites.length}</span></h2>
            <button className="button button-quiet" onClick={() => void loadFavorites()} type="button" disabled={loading}>
              Actualizar
            </button>
          </div>
          <div className="favorite-grid">
            {favorites.map((favorite) => (
              <article className="favorite-card" key={favorite.id}>
                <div className="favorite-heading">
                  <span className="favorite-icon"><Icon name="pin" size={18} /></span>
                  <div>
                    <h3>{favorite.ciudad}</h3>
                    <p>{favorite.pais}</p>
                  </div>
                </div>
                <dl className="coordinate-list">
                  <div><dt>Latitud</dt><dd>{favorite.latitud}</dd></div>
                  <div><dt>Longitud</dt><dd>{favorite.longitud}</dd></div>
                </dl>
                <div className="card-actions">
                  <button
                    className="text-button"
                    onClick={() => void beginEdit(favorite.id)}
                    type="button"
                    disabled={loadingFavoriteId === favorite.id}
                  >
                    <Icon name="edit" size={15} />{loadingFavoriteId === favorite.id ? 'Cargando...' : 'Editar'}
                  </button>
                  <button className="text-button danger-text" onClick={() => setDeleteFavorite(favorite)} type="button">
                    <Icon name="trash" size={15} />Eliminar
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}

      <Modal
        open={formOpen}
        title={editingId === null ? 'Agregar destino favorito' : 'Editar destino'}
        onClose={closeForm}
        closeOnOverlay={!saving}
        footer={
          <>
            <button className="button button-secondary" type="button" onClick={closeForm} disabled={saving}>Cancelar</button>
            <button className="button button-primary" type="submit" form="favorite-form" disabled={saving}>
              {saving ? 'Guardando...' : editingId === null ? 'Guardar destino' : 'Guardar cambios'}
            </button>
          </>
        }
      >
        {error && <ErrorMessage message={error} />}
        <form id="favorite-form" className="form-stack" onSubmit={handleSubmit}>
          <label>
            Ciudad
            <input
              value={form.ciudad}
              onChange={(event) => setForm((current) => ({ ...current, ciudad: event.target.value }))}
              maxLength={100}
              required
            />
          </label>
          <label>
            País
            <input
              value={form.pais}
              onChange={(event) => setForm((current) => ({ ...current, pais: event.target.value }))}
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
                required
              />
            </label>
          </div>
          <p className="field-hint">Usa las coordenadas del lugar para identificarlo con precisión.</p>
        </form>
      </Modal>

      <ConfirmModal
        open={deleteFavorite !== null}
        title="Eliminar destino"
        message={deleteFavorite ? `¿Quieres eliminar ${deleteFavorite.ciudad}, ${deleteFavorite.pais} de tus favoritos?` : ''}
        loading={removing}
        onConfirm={() => void confirmRemove()}
        onClose={() => setDeleteFavorite(null)}
      />
    </section>
  )
}
