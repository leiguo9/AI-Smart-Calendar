# AI Smart Calendar

Stage 1: user authentication and a personal calendar with basic event CRUD.

**Stack:** React (Vite) + [FullCalendar](https://fullcalendar.io/docs/react) frontend · Java Spring Boot backend · PostgreSQL

## What's included

- **Auth:** sign up / sign in with BCrypt password hashing and JWT bearer tokens
- **Calendar:** FullCalendar month / week / day views; click or drag across slots to create, drag or resize events to reschedule
- **Events:** create, list (by date range), edit, and delete — scoped to the signed-in user
- **Docker Compose:** PostgreSQL (and optional full stack)

Out of scope for Stage 1: AI features, sharing, advanced recurrence, mobile apps.

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
| `PUT` | `/api/events/{id}` | yes | Update event |
| `DELETE` | `/api/events/{id}` | yes | Delete event |

Send `Authorization: Bearer <token>` on protected routes.

## Demo notes

1. Open `http://localhost:5173` → **Create an account** (password ≥ 8 characters).
2. You land on the month calendar. Use **New event**, click a day, or drag across days / time slots to add one. Switch to **Week** or **Day** for a time grid.
3. Click an event to edit or delete it, or drag / resize it to reschedule.
4. **Sign out**, then sign back in — your events are still there (per-user, JWT-scoped).

No seed users or secrets are checked in.
