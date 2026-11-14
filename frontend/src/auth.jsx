import { createContext, useContext, useEffect, useState } from 'react'
import { api } from './api'

const AuthContext = createContext(null)
const STORAGE_KEY = 'aiscalendar.auth'

function readStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => readStoredAuth())
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function verify() {
      if (!auth?.token) {
        setReady(true)
        return
      }
      try {
        const me = await api.me(auth.token)
        if (!cancelled) {
          const next = {
            token: auth.token,
            userId: me.userId,
            email: me.email,
            displayName: me.displayName,
          }
          setAuth(next)
          localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
        }
      } catch {
        if (!cancelled) {
          setAuth(null)
          localStorage.removeItem(STORAGE_KEY)
        }
      } finally {
        if (!cancelled) setReady(true)
      }
    }
    verify()
    return () => {
      cancelled = true
    }
  }, [])

  function persist(next) {
    setAuth(next)
    if (next) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  async function signup(payload) {
    const result = await api.signup(payload)
    persist(result)
    return result
  }

  async function login(payload) {
    const result = await api.login(payload)
    persist(result)
    return result
  }

  function logout() {
    persist(null)
  }

  return (
    <AuthContext.Provider value={{ auth, ready, signup, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return value
}
