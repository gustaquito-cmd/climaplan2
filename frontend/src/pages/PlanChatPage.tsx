import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { useNavigate, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../api/errors'
import {
  createPlanChatWebSocketUrl,
  mergePlanChatMessages,
  parsePlanChatMessage,
  planChatApi,
  type PlanChatMessage,
} from '../api/planChat'
import { plansApi } from '../api/plans'
import { ErrorMessage, LoadingState } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { PageHeader } from '../components/PageHeader'
import { PlanChatMessageList } from '../components/PlanChatMessageList'
import { useAuth } from '../context/AuthContext'
import type { Plan } from '../types'

type ConnectionState = 'connecting' | 'connected' | 'disconnected'

function readChatError(message: IMessage): string {
  try {
    const body: unknown = JSON.parse(message.body)
    if (
      typeof body === 'object' &&
      body !== null &&
      'message' in body &&
      typeof body.message === 'string'
    ) {
      return body.message
    }
  } catch {
    return 'No se pudo enviar el mensaje. Inténtalo nuevamente.'
  }
  return 'No se pudo enviar el mensaje. Inténtalo nuevamente.'
}

export function PlanChatPage() {
  const { planId: rawPlanId } = useParams()
  const planId = Number(rawPlanId)
  const validPlanId = Number.isSafeInteger(planId) && planId > 0
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const [plan, setPlan] = useState<Plan | null>(null)
  const [messages, setMessages] = useState<PlanChatMessage[]>([])
  const [connection, setConnection] = useState<ConnectionState>('connecting')
  const [loading, setLoading] = useState(true)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [nextPage, setNextPage] = useState(1)
  const [hasOlderMessages, setHasOlderMessages] = useState(false)
  const [draft, setDraft] = useState('')
  const [pageError, setPageError] = useState('')
  const [chatError, setChatError] = useState('')
  const clientRef = useRef<Client | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)
  const latestMessageIdRef = useRef<number | null>(null)

  const addMessages = useCallback((newMessages: PlanChatMessage[]) => {
    setMessages((current) => mergePlanChatMessages(current, newMessages))
  }, [])

  useEffect(() => {
    if (!validPlanId) {
      setLoading(false)
      setPageError('El identificador del plan no es válido.')
      return
    }

    let active = true
    setLoading(true)
    setPageError('')
    setPlan(null)
    setMessages([])
    setNextPage(1)
    setHasOlderMessages(false)

    Promise.all([plansApi.get(planId), planChatApi.listMessages(planId)])
      .then(([loadedPlan, history]) => {
        if (!active) return
        setPlan(loadedPlan)
        addMessages(history.content)
        setNextPage(history.number + 1)
        setHasOlderMessages(history.number + 1 < history.totalPages)
      })
      .catch((error: unknown) => {
        if (active) setPageError(getApiErrorMessage(error))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [addMessages, planId, validPlanId])

  useEffect(() => {
    if (!validPlanId || !token) {
      setConnection('disconnected')
      return
    }

    let active = true
    setConnection('connecting')
    setChatError('')
    const client = new Client({
      brokerURL: createPlanChatWebSocketUrl(
        import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
        window.location.origin,
      ),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (!active) return
        setConnection('connected')
        client.subscribe(`/topic/plans/${planId}/chat`, (message) => {
          const parsed = parsePlanChatMessage(message.body)
          if (parsed && parsed.planId === planId) addMessages([parsed])
        })
        client.subscribe('/user/queue/chat-errors', (message) => {
          if (active) setChatError(readChatError(message))
        })
      },
      onStompError: (frame) => {
        if (active) {
          setChatError(
            frame.headers.message ||
              'El servicio de chat rechazó la conexión. Verifica tu acceso al plan.',
          )
        }
      },
      onWebSocketClose: () => {
        if (active) setConnection('disconnected')
      },
      onWebSocketError: () => {
        if (active) {
          setChatError('No se pudo conectar al chat. Revisa la conexión con el servidor.')
        }
      },
    })
    clientRef.current = client
    try {
      client.activate()
    } catch {
      if (active) {
        setConnection('disconnected')
        setChatError('No se pudo iniciar la conexión del chat.')
      }
    }

    return () => {
      active = false
      if (clientRef.current === client) clientRef.current = null
      void client.deactivate()
    }
  }, [addMessages, planId, token, validPlanId])

  useEffect(() => {
    const latestMessageId = messages[messages.length - 1]?.id ?? null
    if (latestMessageId !== latestMessageIdRef.current) {
      latestMessageIdRef.current = latestMessageId
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages])

  async function loadOlderMessages() {
    if (loadingOlder || !hasOlderMessages || !validPlanId) return
    setLoadingOlder(true)
    setPageError('')
    try {
      const history = await planChatApi.listMessages(planId, nextPage)
      addMessages(history.content)
      setNextPage(history.number + 1)
      setHasOlderMessages(history.number + 1 < history.totalPages)
    } catch (error) {
      setPageError(getApiErrorMessage(error))
    } finally {
      setLoadingOlder(false)
    }
  }

  function sendMessage(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const content = draft.trim()
    const client = clientRef.current
    if (!content || !client?.connected) return
    setChatError('')
    try {
      client.publish({
        destination: `/app/plans/${planId}/chat`,
        body: JSON.stringify({ content }),
      })
      setDraft('')
    } catch {
      setChatError('No se pudo enviar el mensaje. Inténtalo nuevamente.')
    }
  }

  if (loading) return <LoadingState label="Cargando el chat del plan..." />

  return (
    <section className="plan-chat-page">
      <PageHeader
        title={plan ? `Chat: ${plan.titulo}` : 'Chat del plan'}
        description={plan ? `${plan.ciudad} · ${plan.fecha}` : 'Conversación privada del plan.'}
        action={
          <button
            className="button button-secondary"
            type="button"
            onClick={() => navigate('/plans')}
          >
            Volver a planes
          </button>
        }
      />

      {pageError && <ErrorMessage message={pageError} />}
      {chatError && <ErrorMessage message={chatError} />}

      <section className="plan-chat-panel" aria-label="Chat del plan">
        <header className="plan-chat-header">
          <span className={`chat-connection chat-${connection}`} role="status">
            <span aria-hidden="true" />
            {connection === 'connected'
              ? 'Conectado'
              : connection === 'connecting'
                ? 'Conectando...'
                : 'Desconectado'}
          </span>
          <span className="muted">Solo miembros aceptados pueden participar.</span>
        </header>

        <div className="chat-messages" aria-live="polite" aria-relevant="additions">
          {hasOlderMessages && (
            <button
              className="button button-quiet chat-load-older"
              type="button"
              onClick={() => void loadOlderMessages()}
              disabled={loadingOlder}
            >
              {loadingOlder ? 'Cargando...' : 'Cargar mensajes anteriores'}
            </button>
          )}
          {messages.length === 0 ? (
            <p className="chat-empty">Aún no hay mensajes. Inicia la conversación.</p>
          ) : (
            <PlanChatMessageList messages={messages} userId={user?.id} />
          )}
          <div ref={messagesEndRef} />
        </div>

        <form className="chat-compose" onSubmit={sendMessage}>
          <label className="visually-hidden" htmlFor="plan-chat-message">
            Escribe un mensaje
          </label>
          <textarea
            id="plan-chat-message"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Escribe un mensaje..."
            maxLength={1000}
            rows={2}
            required
            disabled={connection !== 'connected'}
          />
          <button
            className="button button-primary"
            type="submit"
            disabled={connection !== 'connected' || !draft.trim()}
          >
            <Icon name="send" size={16} />
            Enviar
          </button>
        </form>
      </section>
    </section>
  )
}
