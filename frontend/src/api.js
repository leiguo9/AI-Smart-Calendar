/**
 * Thin fetch wrapper around the Spring Boot API.
 *
 * - In Vite dev, leave VITE_API_BASE_URL empty and use the proxy in vite.config.js (`/api` → :8080).
 * - In Docker, the frontend often sets VITE_API_BASE_URL to http://localhost:8080 at build time.
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  // JWT from AuthProvider — backend JwtAuthenticationFilter expects this header.
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // DELETE events returns 204 No Content — no JSON body.
  if (response.status === 204) {
    return null
  }

  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    // Match GlobalExceptionHandler's { error, details? } shape.
    const message = data.error || data.details || `Request failed (${response.status})`
    throw new Error(message)
  }
  return data
}

export const api = {
  signup: (payload) => request('/api/auth/signup', { method: 'POST', body: payload }),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload }),
  me: (token) => request('/api/me', { token }),
  listEvents: (token, from, to) =>
    request(`/api/events?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`, {
      token,
    }),
  createEvent: (token, payload) =>
    request('/api/events', { method: 'POST', body: payload, token }),
  /** One conversational turn; body = { messages, timeZone }. */
  assistantTurn: (token, payload) =>
    request('/api/events/assistant', { method: 'POST', body: payload, token }),
  updateEvent: (token, id, payload) =>
    request(`/api/events/${id}`, { method: 'PUT', body: payload, token }),
  deleteEvent: (token, id) => request(`/api/events/${id}`, { method: 'DELETE', token }),
}
