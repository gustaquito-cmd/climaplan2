import type { PlanStatus } from '../types'

interface PlanStatusFieldProps {
  editing: boolean
  value: PlanStatus
  onChange: (value: PlanStatus) => void
}

function isPlanStatus(value: string): value is PlanStatus {
  return value === 'PENDIENTE' || value === 'COMPLETADO' || value === 'CANCELADO'
}

export function PlanStatusField({ editing, value, onChange }: PlanStatusFieldProps) {
  if (!editing) return null

  return (
    <label>
      Estado
      <select
        value={value}
        onChange={(event) => {
          if (isPlanStatus(event.target.value)) onChange(event.target.value)
        }}
      >
        <option value="PENDIENTE">Pendiente</option>
        <option value="COMPLETADO">Completado</option>
        <option value="CANCELADO">Cancelado</option>
      </select>
    </label>
  )
}
