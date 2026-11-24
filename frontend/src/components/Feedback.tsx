import type { ReactNode } from 'react'

export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="feedback feedback-error" role="alert">
      <span aria-hidden="true">!</span>
      <p>{message}</p>
    </div>
  )
}

export function LoadingState({ label = 'Cargando...' }: { label?: string }) {
  return (
    <div className="loading-state" role="status">
      <span className="loader" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  )
}
