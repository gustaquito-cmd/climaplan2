import { Modal } from './Modal'

export function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = 'Eliminar',
  loading = false,
  onConfirm,
  onClose,
}: {
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  loading?: boolean
  onConfirm: () => void
  onClose: () => void
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      closeOnOverlay={!loading}
      footer={
        <>
          <button className="button button-secondary" type="button" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button className="button button-danger" type="button" onClick={onConfirm} disabled={loading}>
            {loading ? 'Eliminando...' : confirmLabel}
          </button>
        </>
      }
    >
      <p className="confirm-message">{message}</p>
    </Modal>
  )
}
