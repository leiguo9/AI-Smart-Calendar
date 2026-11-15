# AI Smart Calendar

Stage 2: conversational natural-language event creation on top of auth and personal calendar CRUD.

**Stack:** React (Vite) + [FullCalendar](https://fullcalendar.io/docs/react) frontend · Java Spring Boot backend · PostgreSQL · OpenAI (`gpt-4o-mini` tool calling)

## What's included

- **Auth:** sign up / sign in with BCrypt password hashing and JWT bearer tokens
- **Calendar:** FullCalendar month / week / day views; click or drag across slots to create, drag or resize events to reschedule
- **Events:** create, list (by date range), edit, and delete — scoped to the signed-in user
- **Assistant:** side chat panel that asks for missing details (e.g. time), then proposes a draft for preview in the event modal
- **Docker Compose:** PostgreSQL (and optional full stack)

Out of scope for now: recurring series, sharing, mobile apps, persisted chat history.

## Project layout

```
backend/     Spring Boot API (port 8080)
frontend/    React + Vite UI (port 5173)
docker-compose.yml
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 20+
- Docker (for PostgreSQL / full stack), **or** a local PostgreSQL 16 instance
- An OpenAI API key (for the assistant panel)

## Quick start (recommended)

### 1. Start PostgreSQL

```bash
docker compose up -d db
```

Defaults (dev only — change for anything shared):

| Variable | Value |
| --- | --- |
| Database | `aiscalendar` |
| User | `calendar` |
| Password | `calendar` |
| Port | `5432` |

### 2. Run the backend

```bash
cd backend
export OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

API: `http://localhost:8080`

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173`  
The Vite dev server proxies `/api` to the backend.

## Full stack with Docker Compose

```bash
export OPENAI_API_KEY=sk-...
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Postgres: `localhost:5432`

## Configuration

Backend env vars (see `backend/src/main/resources/application.yml`):

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/aiscalendar` | JDBC URL |
| `DATABASE_USER` | `calendar` | DB user |
| `DATABASE_PASSWORD` | `calendar` | DB password |
| `APP_JWT_SECRET` | dev placeholder (≥32 chars) | JWT signing key — **override in real deploys** |
| `APP_CORS_ORIGINS` | `http://localhost:5173` | Allowed browser origins (comma-separated) |
| `OPENAI_API_KEY` | empty | Required for the assistant; missing key → `503` |
| `OPENAI_MODEL` | `gpt-4o-mini` | Chat Completions model |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Override for compatible proxies |

Frontend:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_API_BASE_URL` | empty (same-origin / Vite proxy) | Backend base URL |

## API overview

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/signup` | no | Create account → JWT |
| `POST` | `/api/auth/login` | no | Sign in → JWT |
| `GET` | `/api/me` | yes | Current user |
| `GET` | `/api/events?from=&to=` | yes | List events in range (ISO-8601) |
| `POST` | `/api/events` | yes | Create event |
| `POST` | `/api/events/assistant` | yes | Conversational turn → clarify or propose draft |
| `PUT` | `/api/events/{id}` | yes | Update event |
| `DELETE` | `/api/events/{id}` | yes | Delete event |

Send `Authorization: Bearer <token>` on protected routes.

`POST /api/events/assistant` body:

```json
{
  "messages": [{ "role": "user", "content": "Lunch with Sam tomorrow" }],
  "timeZone": "America/Los_Angeles"
}
```

Response `status` is `need_info` (assistant asks a question) or `ready` (includes a `draft` for the event modal). Recurrence language sets `recurrenceDetected` and a `warning`; only the next occurrence is proposed.

## Demo notes

1. Open `http://localhost:5173` → **Create an account** (password ≥ 8 characters).
2. You land on the month calendar. Use **New event**, click a day, or drag across days / time slots to add one. Switch to **Week** or **Day** for a time grid.
3. Or use the **Assistant** panel: say `Lunch with Sam tomorrow` — it should ask for a time, then open a preview modal when ready.
4. Click an event to edit or delete it, or drag / resize it to reschedule.
5. **Sign out**, then sign back in — your events are still there (per-user, JWT-scoped).

No seed users or secrets are checked in.
