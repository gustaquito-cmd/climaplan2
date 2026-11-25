import api from './client'

export interface PlanChatMessage {
  id: number
  planId: number
  senderId: number
  senderName: string
  content: string
  sentAt: string
}

export interface PlanChatPage {
  content: PlanChatMessage[]
  number: number
  totalPages: number
  totalElements: number
}

export function createPlanChatWebSocketUrl(
  apiBaseUrl: string,
  appOrigin: string,
): string {
  const apiUrl = new URL(apiBaseUrl, appOrigin)
  const websocketUrl = new URL('/ws', apiUrl.origin)
  websocketUrl.protocol = websocketUrl.protocol === 'https:' ? 'wss:' : 'ws:'
  return websocketUrl.toString()
}

export function parsePlanChatMessage(payload: string): PlanChatMessage | null {
  let value: unknown
  try {
    value = JSON.parse(payload)
  } catch {
    return null
  }
  if (typeof value !== 'object' || value === null) return null

  const message = value as Record<string, unknown>
  if (
    typeof message.id !== 'number' ||
    typeof message.planId !== 'number' ||
    typeof message.senderId !== 'number' ||
    typeof message.senderName !== 'string' ||
    typeof message.content !== 'string' ||
    typeof message.sentAt !== 'string'
  ) {
    return null
  }

  return {
    id: message.id,
    planId: message.planId,
    senderId: message.senderId,
    senderName: message.senderName,
    content: message.content,
    sentAt: message.sentAt,
  }
}

export function mergePlanChatMessages(
  current: PlanChatMessage[],
  incoming: PlanChatMessage[],
): PlanChatMessage[] {
  const unique = new Map<number, PlanChatMessage>()
  for (const message of [...current, ...incoming]) {
    unique.set(message.id, message)
  }
  return [...unique.values()].sort((first, second) => {
    const timeDifference =
      new Date(first.sentAt).getTime() - new Date(second.sentAt).getTime()
    return timeDifference || first.id - second.id
  })
}

export const planChatApi = {
  async listMessages(
    planId: number,
    page = 0,
    size = 50,
  ): Promise<PlanChatPage> {
    const response = await api.get<PlanChatPage>(
      `/api/v1/plans/${planId}/messages`,
      { params: { page, size } },
    )
    return response.data
  },
}
