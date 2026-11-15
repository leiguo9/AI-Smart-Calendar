import { useEffect, useState } from 'react'
import { EVENT_CATEGORIES, normalizeCategory } from '../eventCategories.js'

function toLocalInputValue(iso) {
  if (!iso) return ''
  const date = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function defaultRange(seedDate, seedEnd) {
  if (seedDate && seedEnd) {
    return { start: new Date(seedDate), end: new Date(seedEnd) }
  }
  const start = new Date(seedDate || Date.now())
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  return { start, end }
}

export default function EventModal({
  event,
  seedDate,
  seedEnd,
  seedAllDay,
  onClose,
  onSave,
  onDelete,
  busy,
}) {
  const defaults = defaultRange(seedDate, seedEnd)
  const [title, setTitle] = useState(event?.title || '')
  const [description, setDescription] = useState(event?.description || '')
  const [startAt, setStartAt] = useState(
    toLocalInputValue(event?.startAt) || toLocalInputValue(defaults.start.toISOString()),
  )
  const [endAt, setEndAt] = useState(
    toLocalInputValue(event?.endAt) || toLocalInputValue(defaults.end.toISOString()),
  )
  const [allDay, setAllDay] = useState(Boolean(event ? event.allDay : seedAllDay))
  const [category, setCategory] = useState(normalizeCategory(event?.category))
  const [error, setError] = useState('')

  useEffect(() => {
    function onKey(e) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const start = new Date(startAt)
    const end = new Date(endAt)
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      setError('Enter valid start and end times')
      return
    }
    if (!(end > start)) {
      setError('End must be after start')
      return
    }
    try {
      await onSave({
        title: title.trim(),
        description: description.trim() || null,
        startAt: start.toISOString(),
        endAt: end.toISOString(),
        allDay,
        category,
      })
    } catch (err) {
      setError(err.message || 'Could not save event')
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="event-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="event-modal-title">{event ? 'Edit event' : 'New event'}</h2>
          <button type="button" className="btn ghost" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <form className="event-form" onSubmit={handleSubmit}>
          <label>
            Title
            <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
          </label>
          <label>
            Description
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
          <div className="field-row">
            <label>
              Starts
              <input
                type="datetime-local"
                value={startAt}
                onChange={(e) => setStartAt(e.target.value)}
                required
              />
            </label>
            <label>
              Ends
              <input
                type="datetime-local"
                value={endAt}
                onChange={(e) => setEndAt(e.target.value)}
                required
              />
            </label>
          </div>
          <label>
            Category
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              {EVENT_CATEGORIES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={allDay}
              onChange={(e) => setAllDay(e.target.checked)}
            />
            All-day event
          </label>
          {error && <p className="form-error">{error}</p>}
          <div className="modal-actions">
            {event && (
              <button
                type="button"
                className="btn danger"
                disabled={busy}
                onClick={() => onDelete(event)}
              >
                Delete
              </button>
            )}
            <div className="spacer" />
            <button type="button" className="btn ghost" onClick={onClose} disabled={busy}>
              Cancel
            </button>
            <button type="submit" className="btn primary" disabled={busy}>
              {busy ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
