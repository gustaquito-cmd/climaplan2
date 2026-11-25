import type { PlanChatMessage } from '../api/planChat'

function formatMessageTime(sentAt: string): string {
  const date = new Date(sentAt)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('es-MX', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function PlanChatMessageList({
  messages,
  userId,
}: {
  messages: PlanChatMessage[]
  userId: number | undefined
}) {
  return (
    <>
      {messages.map((message) => {
        const ownMessage = message.senderId === userId
        return (
          <article
            className={`chat-message${ownMessage ? ' chat-message-own' : ''}`}
            key={message.id}
          >
            <div className="chat-message-heading">
              <strong>{ownMessage ? 'Tú' : message.senderName}</strong>
              <time dateTime={message.sentAt}>{formatMessageTime(message.sentAt)}</time>
            </div>
            <p>{message.content}</p>
          </article>
        )
      })}
    </>
  )
}
