import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { getApiErrorMessage } from '../api/errors'
import { usersAdminApi, type UsuarioAdminUpdate } from '../api/usersAdmin'
import { ConfirmModal } from '../components/ConfirmModal'
import { EmptyState, ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { PageHeader } from '../components/PageHeader'
import { useToast } from '../context/ToastContext'
import type { Usuario } from '../types'

type UserModalMode = 'view' | 'edit' | null

function getErrorStatus(error: unknown): number | null {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return null
  }
  const response = error.response
  if (typeof response !== 'object' || response === null || !('status' in response)) {
    return null
  }
  return typeof response.status === 'number' ? response.status : null
}

function getAdminErrorMessage(error: unknown): string {
  const status = getErrorStatus(error)
  if (status === 401) return ''
  if (status === 403) return 'No tienes permisos para acceder a esta sección.'
  if (status === 500) return 'El servidor tuvo un problema. Inténtalo más tarde.'
  return getApiErrorMessage(error)
}

function roleLabel(role: Usuario['rol']) {
  return role === 'ROLE_ADMIN' ? 'Administrador' : 'Usuario'
}

function statusLabel(status: Usuario['estado']) {
  return status === 'ACTIVO' ? 'Activo' : 'Inactivo'
}

export function AdminPage() {
  const { showToast } = useToast()
  const [users, setUsers] = useState<Usuario[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [modalError, setModalError] = useState('')
  const [modalMode, setModalMode] = useState<UserModalMode>(null)
  const [selectedUser, setSelectedUser] = useState<Usuario | null>(null)
  const [updateForm, setUpdateForm] = useState<UsuarioAdminUpdate>({ nombre: '', email: '' })
  const [loadingUserId, setLoadingUserId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [confirmUser, setConfirmUser] = useState<Usuario | null>(null)
  const [deactivating, setDeactivating] = useState(false)

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      setUsers(await usersAdminApi.list())
    } catch (requestError) {
      setLoadError(getAdminErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadUsers()
  }, [loadUsers])

  async function openUser(id: number, mode: Exclude<UserModalMode, null>) {
    setModalError('')
    setSelectedUser(null)
    setModalMode(mode)
    setLoadingUserId(id)
    try {
      const user = await usersAdminApi.get(id)
      setSelectedUser(user)
      if (mode === 'edit') {
        setUpdateForm({ nombre: user.nombre, email: user.email })
      }
    } catch (requestError) {
      setModalError(getAdminErrorMessage(requestError))
    } finally {
      setLoadingUserId(null)
    }
  }

  function closeUserModal() {
    if (saving) return
    setModalMode(null)
    setSelectedUser(null)
    setModalError('')
  }

  function beginEditSelectedUser() {
    if (!selectedUser) return
    setUpdateForm({ nombre: selectedUser.nombre, email: selectedUser.email })
    setModalError('')
    setModalMode('edit')
  }

  async function saveUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedUser) return
    setSaving(true)
    setModalError('')
    try {
      const updated = await usersAdminApi.update(selectedUser.id, updateForm)
      setUsers((current) => current.map((user) => user.id === updated.id ? updated : user))
      setSelectedUser(updated)
      setModalMode(null)
      showToast('Usuario actualizado correctamente.')
    } catch (requestError) {
      setModalError(getAdminErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function deactivateUser() {
    if (!confirmUser) return
    setDeactivating(true)
    setActionError('')
    try {
      await usersAdminApi.deactivate(confirmUser.id)
      setUsers((current) => current.map((user) =>
        user.id === confirmUser.id ? { ...user, estado: 'INACTIVO' } : user,
      ))
      setConfirmUser(null)
      showToast('Usuario desactivado. Sus datos se conservaron.')
    } catch (requestError) {
      setActionError(getAdminErrorMessage(requestError))
      setConfirmUser(null)
    } finally {
      setDeactivating(false)
    }
  }

  const stats = [
    { label: 'Total de usuarios', value: users.length },
    { label: 'Activos', value: users.filter((user) => user.estado === 'ACTIVO').length },
    { label: 'Inactivos', value: users.filter((user) => user.estado === 'INACTIVO').length },
    { label: 'Administradores', value: users.filter((user) => user.rol === 'ROLE_ADMIN').length },
    { label: 'Usuarios normales', value: users.filter((user) => user.rol === 'ROLE_USER').length },
  ]

  const rows = users.map((user) => (
    <tr key={user.id}>
      <td>{user.id}</td>
      <td><strong>{user.nombre}</strong></td>
      <td>{user.email}</td>
      <td><span className={`admin-badge role-${user.rol.toLowerCase()}`}>{roleLabel(user.rol)}</span></td>
      <td><span className={`admin-badge state-${user.estado.toLowerCase()}`}>{statusLabel(user.estado)}</span></td>
      <td>
        <div className="admin-actions">
          <button
            className="text-button"
            type="button"
            onClick={() => void openUser(user.id, 'view')}
            disabled={loadingUserId !== null}
            aria-label={`Ver usuario ${user.nombre}`}
          >
            <Icon name="eye" size={15} /> Ver
          </button>
          <button
            className="text-button"
            type="button"
            onClick={() => void openUser(user.id, 'edit')}
            disabled={loadingUserId !== null}
            aria-label={`Editar usuario ${user.nombre}`}
          >
            <Icon name="edit" size={15} /> Editar
          </button>
          {user.estado === 'ACTIVO' ? (
            <button
              className="text-button danger-text"
              type="button"
              onClick={() => {
                setActionError('')
                setConfirmUser(user)
              }}
              disabled={deactivating}
              aria-label={`Desactivar usuario ${user.nombre}`}
            >
              <Icon name="trash" size={15} /> Desactivar
            </button>
          ) : (
            <span className="admin-action-muted">Desactivado</span>
          )}
        </div>
      </td>
    </tr>
  ))

  return (
    <section>
      <PageHeader
        title="Administración"
        description="Gestión de usuarios de ClimaPlan."
        action={
          <button className="button button-secondary" type="button" onClick={() => void loadUsers()} disabled={loading}>
            {loading ? 'Actualizando...' : 'Actualizar'}
          </button>
        }
      />

      {actionError && <ErrorMessage message={actionError} />}

      {loading && users.length === 0 ? (
        <LoadingState label="Cargando usuarios..." />
      ) : loadError && users.length === 0 ? (
        <div>
          <ErrorMessage message={loadError} />
          {loadError && (
            <button className="button button-secondary" type="button" onClick={() => void loadUsers()}>
              Reintentar
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="admin-stats">
            {stats.map((stat) => (
              <article className="admin-stat-card" key={stat.label}>
                <span>{stat.label}</span>
                <strong>{stat.value}</strong>
              </article>
            ))}
          </div>

          {loadError && <ErrorMessage message={loadError} />}

          {users.length === 0 ? (
            <EmptyState title="No hay usuarios" description="No se encontraron cuentas para mostrar." />
          ) : (
            <section className="admin-user-section" aria-label="Usuarios">
              <div className="admin-list-heading">
                <h2>Usuarios</h2>
                <span>{users.length} registros</span>
              </div>

              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th scope="col">ID</th>
                      <th scope="col">Nombre</th>
                      <th scope="col">Email</th>
                      <th scope="col">Rol</th>
                      <th scope="col">Estado</th>
                      <th scope="col">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>{rows}</tbody>
                </table>
              </div>

              <div className="admin-mobile-list">
                {users.map((user) => (
                  <article className="admin-user-card" key={user.id}>
                    <div className="admin-user-card-heading">
                      <div>
                        <h3>{user.nombre}</h3>
                        <p>{user.email}</p>
                      </div>
                      <span className="admin-user-id">#{user.id}</span>
                    </div>
                    <div className="admin-user-meta">
                      <span className={`admin-badge role-${user.rol.toLowerCase()}`}>{roleLabel(user.rol)}</span>
                      <span className={`admin-badge state-${user.estado.toLowerCase()}`}>{statusLabel(user.estado)}</span>
                    </div>
                    <div className="admin-actions">
                      <button className="text-button" type="button" onClick={() => void openUser(user.id, 'view')} disabled={loadingUserId !== null}>
                        <Icon name="eye" size={15} /> Ver
                      </button>
                      <button className="text-button" type="button" onClick={() => void openUser(user.id, 'edit')} disabled={loadingUserId !== null}>
                        <Icon name="edit" size={15} /> Editar
                      </button>
                      {user.estado === 'ACTIVO' ? (
                        <button
                          className="text-button danger-text"
                          type="button"
                          onClick={() => {
                            setActionError('')
                            setConfirmUser(user)
                          }}
                          disabled={deactivating}
                        >
                          <Icon name="trash" size={15} /> Desactivar
                        </button>
                      ) : (
                        <span className="admin-action-muted">Desactivado</span>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </section>
          )}
        </>
      )}

      <Modal
        open={modalMode !== null}
        title={modalMode === 'edit' ? 'Editar usuario' : 'Detalle del usuario'}
        onClose={closeUserModal}
        closeOnOverlay={!saving}
        footer={
          modalMode === 'edit' ? (
            <>
              <button className="button button-secondary" type="button" onClick={closeUserModal} disabled={saving}>
                Cancelar
              </button>
              <button className="button button-primary" type="submit" form="admin-user-form" disabled={saving || !selectedUser}>
                {saving ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </>
          ) : selectedUser ? (
            <>
              <button className="button button-secondary" type="button" onClick={closeUserModal}>Cerrar</button>
              <button className="button button-primary" type="button" onClick={beginEditSelectedUser}>Editar</button>
            </>
          ) : null
        }
      >
        {modalError && <ErrorMessage message={modalError} />}
        {loadingUserId !== null ? (
          <LoadingState label="Cargando usuario..." />
        ) : modalMode === 'edit' ? (
          <form id="admin-user-form" className="form-stack" onSubmit={(event) => void saveUser(event)}>
            <label>
              Nombre
              <input
                value={updateForm.nombre}
                onChange={(event) => setUpdateForm((current) => ({ ...current, nombre: event.target.value }))}
                required
              />
            </label>
            <label>
              Email
              <input
                type="email"
                value={updateForm.email}
                onChange={(event) => setUpdateForm((current) => ({ ...current, email: event.target.value }))}
                required
              />
            </label>
          </form>
        ) : selectedUser ? (
          <dl className="detail-list">
            <div><dt>ID</dt><dd>{selectedUser.id}</dd></div>
            <div><dt>Nombre</dt><dd>{selectedUser.nombre}</dd></div>
            <div><dt>Email</dt><dd>{selectedUser.email}</dd></div>
            <div><dt>Rol</dt><dd>{roleLabel(selectedUser.rol)}</dd></div>
            <div><dt>Estado</dt><dd>{statusLabel(selectedUser.estado)}</dd></div>
          </dl>
        ) : null}
      </Modal>

      <ConfirmModal
        open={confirmUser !== null}
        title="Desactivar usuario"
        message={confirmUser
          ? `La cuenta de ${confirmUser.nombre} se marcará como inactiva. Sus datos no se eliminarán.`
          : ''}
        confirmLabel="Desactivar"
        loading={deactivating}
        onConfirm={() => void deactivateUser()}
        onClose={() => setConfirmUser(null)}
      />
    </section>
  )
}
