import { useCallback, useRef, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/react/daygrid'
import interactionPlugin from '@fullcalendar/react/interaction'
import pulseThemePlugin from '@fullcalendar/react/themes/pulse'
import timeGridPlugin from '@fullcalendar/react/timegrid'
import '@fullcalendar/react/skeleton.css'
import '@fullcalendar/react/themes/pulse/theme.css'
import '@fullcalendar/react/themes/pulse/palettes/green.css'
import { api } from '../api'
import { useAuth } from '../auth.jsx'
import EventModal from '../components/EventModal.jsx'
import { colorsForCategory, normalizeCategory } from '../eventCategories.js'

const DAY_MS = 24 * 60 * 60 * 1000
const HOUR_MS = 60 * 60 * 1000

const PLUGINS = [pulseThemePlugin, dayGridPlugin, timeGridPlugin, interactionPlugin]

const HEADER_TOOLBAR = {
  left: 'prev,next today',
  center: 'title',
  right: 'dayGridMonth,timeGridWeek,timeGridDay',
}

const EVENT_TIME_FORMAT = { hour: 'numeric', minute: '2-digit', meridiem: 'short' }

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

// All-day events are painted on every local day they touch; FullCalendar wants an exclusive midnight end.
function allDayEnd(end) {
  const day = startOfDay(end)
  if (end.getTime() !== day.getTime()) day.setDate(day.getDate() + 1)
  return day
}

function toCalendarEvent(event) {
  // Pulse theme paints via --fc-event-color from the `color` prop (not backgroundColor).
  const colors = colorsForCategory(event.category)
  const base = {
    id: event.id,
    title: event.title,
    color: colors.color,
    contrastColor: colors.contrast,
    extendedProps: { source: event },
  }
  if (!event.allDay) {
    return { ...base, start: event.startAt, end: event.endAt }
  }
  const start = startOfDay(new Date(event.startAt))
  const end = allDayEnd(new Date(event.endAt))
  return { ...base, allDay: true, start, end: end > start ? end : new Date(start.getTime() + DAY_MS) }
}

export default function CalendarPage() {
  const { auth, logout } = useAuth()
  const calendarRef = useRef(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)

  const fetchEvents = useCallback(
    async (info) => {
      try {
        const data = await api.listEvents(auth.token, info.start.toISOString(), info.end.toISOString())
        setError('')
        return data.map(toCalendarEvent)
      } catch (err) {
        setError(err.message || 'Failed to load events')
        throw err
      }
    },
    [auth.token],
  )

  function refetch() {
    calendarRef.current?.getApi().refetchEvents()
  }

  function handleSelect(info) {
    info.view.calendar.unselect()
    const singleMonthDay =
      info.view.type === 'dayGridMonth' && info.end.getTime() - info.start.getTime() <= DAY_MS
    if (singleMonthDay) {
      setModal({ event: null, seedDate: info.start })
    } else {
      setModal({ event: null, seedDate: info.start, seedEnd: info.end, seedAllDay: info.allDay })
    }
  }

  function handleEventClick(info) {
    setModal({ event: info.event.extendedProps.source })
  }

  async function handleEventChange(info) {
    const { event } = info
    const source = event.extendedProps.source
    const start = event.start
    const end = event.end ?? new Date(start.getTime() + (event.allDay ? DAY_MS : HOUR_MS))
    setError('')
    try {
      await api.updateEvent(auth.token, event.id, {
        title: source.title,
        description: source.description,
        startAt: start.toISOString(),
        endAt: end.toISOString(),
        allDay: event.allDay,
        category: normalizeCategory(source.category),
      })
      refetch()
    } catch (err) {
      info.revert()
      setError(err.message || 'Failed to move event')
    }
  }

  async function saveEvent(payload) {
    setBusy(true)
    try {
      if (modal?.event) {
        await api.updateEvent(auth.token, modal.event.id, payload)
      } else {
        await api.createEvent(auth.token, payload)
      }
      setModal(null)
      refetch()
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
      refetch()
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
        <div className="toolbar-actions">
          <button
            type="button"
            className="btn primary"
            onClick={() => setModal({ event: null, seedDate: new Date() })}
          >
            New event
          </button>
          <button type="button" className="btn ghost" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>

      {error && <p className="banner error">{error}</p>}
      {loading && <p className="banner">Loading events…</p>}

      <section className="calendar-card">
        <FullCalendar
          ref={calendarRef}
          plugins={PLUGINS}
          initialView="dayGridMonth"
          headerToolbar={HEADER_TOOLBAR}
          events={fetchEvents}
          loading={setLoading}
          // Pulse month view defaults timed events to list-item (tiny dot); block keeps category bars.
          eventDisplay="block"
          selectable
          selectMirror
          editable
          dayMaxEvents={3}
          nowIndicator
          scrollTime="08:00:00"
          eventTimeFormat={EVENT_TIME_FORMAT}
          select={handleSelect}
          eventClick={handleEventClick}
          eventDrop={handleEventChange}
          eventResize={handleEventChange}
        />
      </section>

      {modal && (
        <EventModal
          event={modal.event}
          seedDate={modal.seedDate}
          seedEnd={modal.seedEnd}
          seedAllDay={modal.seedAllDay}
          busy={busy}
          onClose={() => setModal(null)}
          onSave={saveEvent}
          onDelete={deleteEvent}
        />
      )}
    </div>
  )
}
