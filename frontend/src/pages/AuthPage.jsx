import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'

/** Shared login/signup form. `mode` is "login" or "signup" from App routes. */
export default function AuthPage({ mode }) {
  const isSignup = mode === 'signup'
  const { login, signup } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (isSignup) {
        await signup({ displayName, email, password })
      } else {
        await login({ email, password })
      }
      navigate('/')
    } catch (err) {
      setError(err.message || 'Something went wrong')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-panel">
        <p className="brand">AI Smart Calendar</p>
        <h1>{isSignup ? 'Create your account' : 'Welcome back'}</h1>
        <p className="lede">
          {isSignup
            ? 'Sign up to manage your events in one place.'
            : 'Sign in to open your calendar.'}
        </p>

        <form className="auth-form" onSubmit={onSubmit}>
          {isSignup && (
            <label>
              Display name
              <input
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                autoComplete="name"
                required
                maxLength={100}
              />
            </label>
          )}
          <label>
            Email
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={isSignup ? 'new-password' : 'current-password'}
              required
              minLength={8}
            />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button type="submit" className="btn primary" disabled={busy}>
            {busy ? 'Working…' : isSignup ? 'Sign up' : 'Sign in'}
          </button>
        </form>

        <p className="auth-switch">
          {isSignup ? (
            <>
              Already have an account? <Link to="/login">Sign in</Link>
            </>
          ) : (
            <>
              New here? <Link to="/signup">Create an account</Link>
            </>
          )}
        </p>
      </div>
    </div>
  )
}
