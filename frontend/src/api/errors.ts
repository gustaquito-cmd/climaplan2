const statusMessages: Record<number, string> = {
  400: 'Revisa los datos ingresados.',
  401: 'Tu sesión no es válida. Inicia sesión nuevamente.',
  403: 'No tienes permisos para realizar esta acción.',
  404: 'No se encontró el recurso solicitado.',
  409: 'Ya existe un registro con esos datos.',
  500: 'El servidor tuvo un problema. Inténtalo más tarde.',
}

function asRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function getApiErrorMessage(error: unknown): string {
  const root = asRecord(error) ? error : null
  const response = asRecord(root?.response) ? root.response : null
  const data = asRecord(response?.data) ? response.data : null

  if (!response) {
    return 'No se pudo conectar con el servidor. Verifica tu conexión e inténtalo de nuevo.'
  }

  const status = typeof response.status === 'number' ? response.status : 0
  const backendMessage =
    typeof data?.message === 'string' && data.message.trim()
      ? data.message
      : statusMessages[status] || 'Ocurrió un error inesperado.'

  const validationErrors = asRecord(data?.validationErrors)
    ? data.validationErrors
    : null
  const details = validationErrors
    ? Object.entries(validationErrors)
        .filter((entry): entry is [string, string] => typeof entry[1] === 'string')
        .map(([field, message]) => `${field}: ${message}`)
    : []

  return details.length ? `${backendMessage} ${details.join(' ')}` : backendMessage
}
