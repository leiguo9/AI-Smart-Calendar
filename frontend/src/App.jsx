import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import AuthPage from './pages/AuthPage.jsx'
import CalendarPage from './pages/CalendarPage.jsx'

function Protected({ children }) {
  const { auth, ready } = useAuth()
  if (!ready) {
    return <div className="boot">Loading…</div>
  }
  if (!auth) {
    return <Navigate to="/login" replace />
  }
  return children
}

function PublicOnly({ children }) {
  const { auth, ready } = useAuth()
  if (!ready) {
    return <div className="boot">Loading…</div>
  }
  if (auth) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnly>
            <AuthPage mode="login" />
          </PublicOnly>
        }
      />
      <Route
        path="/signup"
        element={
          <PublicOnly>
            <AuthPage mode="signup" />
          </PublicOnly>
        }
      />
      <Route
        path="/"
        element={
          <Protected>
            <CalendarPage />
          </Protected>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
