import { useEffect, useRef, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth.jsx'

/**
 * Side chat UI for natural-language event creation.
 *
 * Conversation state lives here (not on the server). Each Send posts the full `messages`
 * array to POST /api/events/assistant. Parent CalendarPage handles `onPropose(draft)`.
 * Remount with a new `key` after a successful save to clear the thread.
 */
export default function EventAssistantPanel({ onPropose }) {
  const { auth } = useAuth()
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const listRef = useRef(null)

  // Keep the latest message visible after each turn.
  useEffect(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, busy])

  async function handleSubmit(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || busy) return

    // Optimistic UI: show the user bubble before the network returns.
    const nextMessages = [...messages, { role: 'user', content: text }]
    setMessages(nextMessages)
    setInput('')
    setBusy(true)
    setError('')

    try {
      const result = await api.assistantTurn(auth.token, {
        messages: nextMessages,
        // Browser zone so the model can resolve "tomorrow at 3pm" correctly.
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
      const assistantMessage =
        result.assistantMessage?.trim() ||
        (result.status === 'ready'
          ? 'Draft ready — review it in the form.'
          : 'Could you share a bit more detail?')
      setMessages((prev) => [...prev, { role: 'assistant', content: assistantMessage }])
      if (result.status === 'ready' && result.draft) {
        onPropose(result.draft)
      }
    } catch (err) {
      setError(err.message || 'Assistant request failed')
      // Roll back the optimistic user message so they can retry.
      setMessages((prev) => prev.slice(0, -1))
      setInput(text)
    } finally {
      setBusy(false)
    }
  }

  function handleClear() {
    setMessages([])
    setInput('')
    setError('')
  }

  return (
    <aside className="assistant-panel" aria-label="Event assistant">
      <div className="assistant-header">
        <div>
          <h2>Assistant</h2>
          <p>Describe an event — I’ll ask if anything’s missing.</p>
        </div>
        <button
          type="button"
          className="btn ghost"
          onClick={handleClear}
          disabled={busy || messages.length === 0}
        >
          Clear
        </button>
      </div>

      <div className="assistant-messages" ref={listRef}>
        {messages.length === 0 && (
          <p className="assistant-empty">
            Try “Lunch with Sam tomorrow” or “Team standup next Monday at 9am”.
          </p>
        )}
        {messages.map((msg, index) => (
          <div key={`${msg.role}-${index}`} className={`assistant-bubble ${msg.role}`}>
            {msg.content}
          </div>
        ))}
        {busy && <p className="assistant-status">Thinking…</p>}
      </div>

      {error && <p className="form-error assistant-error">{error}</p>}

      <form className="assistant-composer" onSubmit={handleSubmit}>
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Message the assistant…"
          rows={2}
          maxLength={500}
          disabled={busy}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              e.currentTarget.form?.requestSubmit()
            }
          }}
        />
        <button type="submit" className="btn primary" disabled={busy || !input.trim()}>
          Send
        </button>
      </form>
    </aside>
  )
}
