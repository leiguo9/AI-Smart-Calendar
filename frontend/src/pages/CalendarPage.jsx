import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth.jsx'
import EventModal from '../components/EventModal.jsx'

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

function startOfMonth(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function addMonths(date, delta) {
  return new Date(date.getFullYear(), date.getMonth() + delta, 1)
}

function monthLabel(date) {
  return date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
}

function buildCells(monthDate) {
  const first = startOfMonth(monthDate)
  const startOffset = first.getDay()
  const gridStart = new Date(first)
  gridStart.setDate(first.getDate() - startOffset)

  const cells = []
  for (let i = 0; i < 42; i += 1) {
    const day = new Date(gridStart)
    day.setDate(gridStart.getDate() + i)
    cells.push(day)
  }
  return cells
}

function dayKey(date) {
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
}

function sameDay(a, b) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString(undefined, {
    hour: 'numeric',
    minute: '2-digit',
  })
}

function rangeForMonth(monthDate) {
  const from = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1)
  const to = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 1)
  // Include trailing/leading grid days
  from.setDate(from.getDate() - from.getDay())
  to.setDate(to.getDate() + (6 - new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDay()))
  return { from: from.toISOString(), to: to.toISOString() }
}

export default function CalendarPage() {
  const { auth, logout } = useAuth()
  const [month, setMonth] = useState(() => startOfMonth(new Date()))
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)

  const cells = useMemo(() => buildCells(month), [month])
  const today = useMemo(() => new Date(), [])

  const eventsByDay = useMemo(() => {
    const map = new Map()
    for (const event of events) {
      const start = new Date(event.startAt)
      const end = new Date(event.endAt)
      const cursor = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const last = new Date(end.getFullYear(), end.getMonth(), end.getDate())
      // If event ends at midnight, don't paint that day
      if (end.getHours() === 0 && end.getMinutes() === 0 && end.getSeconds() === 0) {
        last.setDate(last.getDate() - 1)
      }
      while (cursor <= last) {
        const key = dayKey(cursor)
        if (!map.has(key)) map.set(key, [])
        map.get(key).push(event)
        cursor.setDate(cursor.getDate() + 1)
      }
    }
    for (const list of map.values()) {
      list.sort((a, b) => new Date(a.startAt) - new Date(b.startAt))
    }
    return map
  }, [events])

  async function loadEvents() {
    setLoading(true)
    setError('')
    try {
      const { from, to } = rangeForMonth(month)
      const data = await api.listEvents(auth.token, from, to)
      setEvents(data)
    } catch (err) {
      setError(err.message || 'Failed to load events')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadEvents()
  }, [month, auth.token])

  async function saveEvent(payload) {
    setBusy(true)
    try {
      if (modal?.event) {
        await api.updateEvent(auth.token, modal.event.id, payload)
      } else {
        await api.createEvent(auth.token, payload)
      }
      setModal(null)
      await loadEvents()
    } finally {
      setBusy(false)
    }
  }

  async function deleteEvent(event) {
    if (!window.confirm(`Delete “${event.title}”?`)) return
    setBusy(true)
    try {
      await api.deleteEvent(auth.token, event.id)
      setModal(null)
      await loadEvents()
    } catch (err) {
      setError(err.message || 'Failed to delete event')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="brand compact">AI Smart Calendar</p>
          <p className="welcome">Hi, {auth.displayName}</p>
        </div>
        <button type="button" className="btn ghost" onClick={logout}>
          Sign out
        </button>
      </header>

      <section className="calendar-toolbar">
        <div className="month-nav">
          <button type="button" className="btn ghost" onClick={() => setMonth((m) => addMonths(m, -1))}>
            ‹
          </button>
          <h1>{monthLabel(month)}</h1>
          <button type="button" className="btn ghost" onClick={() => setMonth((m) => addMonths(m, 1))}>
            ›
          </button>
        </div>
        <div className="toolbar-actions">
          <button type="button" className="btn ghost" onClick={() => setMonth(startOfMonth(new Date()))}>
            Today
          </button>
          <button
            type="button"
            className="btn primary"
            onClick={() => setModal({ event: null, seedDate: new Date() })}
          >
            New event
          </button>
        </div>
      </section>

      {error && <p className="banner error">{error}</p>}
      {loading && <p className="banner">Loading events…</p>}

      <div className="weekday-row">
        {WEEKDAYS.map((day) => (
          <div key={day} className="weekday">
            {day}
          </div>
        ))}
      </div>

      <div className="month-grid">
        {cells.map((day) => {
          const inMonth = day.getMonth() === month.getMonth()
          const key = dayKey(day)
          const dayEvents = eventsByDay.get(key) || []
          return (
            <button
              type="button"
              key={key}
              className={[
                'day-cell',
                inMonth ? '' : 'muted',
                sameDay(day, today) ? 'today' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => setModal({ event: null, seedDate: day })}
            >
              <span className="day-number">{day.getDate()}</span>
              <div className="day-events">
                {dayEvents.slice(0, 3).map((event) => (
                  <span
                    key={event.id}
                    className="event-chip"
                    title={event.title}
                    onClick={(e) => {
                      e.stopPropagation()
                      setModal({ event, seedDate: day })
                    }}
                  >
                    {!event.allDay && <em>{formatTime(event.startAt)}</em>}
                    {event.title}
                  </span>
                ))}
                {dayEvents.length > 3 && (
                  <span className="more">+{dayEvents.length - 3} more</span>
                )}
              </div>
            </button>
          )
        })}
      </div>

      {modal && (
        <EventModal
          event={modal.event}
          seedDate={modal.seedDate}
          busy={busy}
          onClose={() => setModal(null)}
          onSave={saveEvent}
          onDelete={deleteEvent}
        />
      )}
    </div>
  )
}
