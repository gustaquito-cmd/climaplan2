import { describe, expect, it } from 'vitest'
import {
  createPlanChatWebSocketUrl,
  mergePlanChatMessages,
  parsePlanChatMessage,
} from './planChat'

describe('plan chat API helpers', () => {
  it('creates a secure WebSocket URL from the API origin', () => {
    expect(
      createPlanChatWebSocketUrl(
        'https://api.example.com/api',
        'https://app.example.com',
      ),
    ).toBe('wss://api.example.com/ws')
  })

  it('uses the app origin when the API base URL is relative', () => {
    expect(createPlanChatWebSocketUrl('/', 'http://localhost:5173')).toBe(
      'ws://localhost:5173/ws',
    )
  })

  it('accepts only well-formed chat messages', () => {
    const payload = JSON.stringify({
      id: 8,
      planId: 4,
      senderId: 2,
      senderName: 'Ana',
      content: '<script>alert(1)</script>',
      sentAt: '2026-11-24T12:00:00Z',
    })

    expect(parsePlanChatMessage(payload)?.content).toBe(
      '<script>alert(1)</script>',
    )
    expect(parsePlanChatMessage('{invalid')).toBeNull()
    expect(parsePlanChatMessage('{"id":"invalid"}')).toBeNull()
  })

  it('deduplicates echoed messages and keeps chronological order', () => {
    const first = {
      id: 1,
      planId: 4,
      senderId: 2,
      senderName: 'Ana',
      content: 'Primero',
      sentAt: '2026-11-24T12:00:00Z',
    }
    const second = {
      ...first,
      id: 2,
      content: 'Segundo',
      sentAt: '2026-11-24T12:01:00Z',
    }

    expect(mergePlanChatMessages([second], [first, second])).toEqual([
      first,
      second,
    ])
  })
})
