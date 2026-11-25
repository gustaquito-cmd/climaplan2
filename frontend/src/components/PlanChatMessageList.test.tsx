import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { PlanChatMessageList } from './PlanChatMessageList'

describe('PlanChatMessageList', () => {
  it('renders chat content as text and identifies the current user', () => {
    const markup = renderToStaticMarkup(
      <PlanChatMessageList
        userId={9}
        messages={[
          {
            id: 1,
            planId: 7,
            senderId: 9,
            senderName: 'Gustavo',
            content: '<script>alert(1)</script>',
            sentAt: '2026-11-24T18:00:00Z',
          },
        ]}
      />,
    )

    expect(markup).toContain('Tú')
    expect(markup).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(markup).not.toContain('<script>')
  })
})
