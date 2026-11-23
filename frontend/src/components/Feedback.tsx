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
