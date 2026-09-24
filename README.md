# Support Ticket Management System

Spec-driven Support Ticket Management System built with **Java 21 / Spring Boot**, **PostgreSQL**, and **Next.js**.

Specifications live in [`spec/`](spec/). Implementation follows the approved plan in [`spec/implementation-plan.md`](spec/implementation-plan.md).

## Project status

| Gate | Milestone | Status |
|------|-----------|--------|
| GATE 10 | Backend API, persistence, state machine | **Complete** |
| GATE 11 | Frontend UI (list, create, detail, status, comments) | **Complete** |
| GATE 12 | E2E verification | **Complete** — [`docs/e2e-run-log.md`](docs/e2e-run-log.md) |
| GATE 13 | Final review & documentation | **Complete** |

**Test counts (final):** 82 backend tests (`./mvnw test`), 5 frontend vitest cases + production build.

## Features

- Create, list, search, and filter support tickets
- Update ticket fields (title, description, priority, assignee)
- Status workflow: Open → In Progress → Resolved → Closed (with Cancel paths)
- Threaded comments per ticket
- Interactive Next.js UI with live search, status pipeline, and card-based list

## Prerequisites

- **Java 21**
- **Docker** and **Docker Compose**
- **Node.js LTS** (for frontend)

## Quick start

### 1. Environment

```bash
cp .env.example .env
```

Edit `.env` if you need non-default credentials. **Never commit `.env`.**

### 2. PostgreSQL

```bash
docker compose up -d
docker compose ps   # wait until healthy
```

| Service    | Host      | Port | Database          |
|------------|-----------|------|-------------------|
| PostgreSQL | localhost | 5432 | `support_tickets` |

Stop (data persists in Docker volume `postgres_data`):

```bash
docker compose down
```

### 3. Backend API

```bash
cd backend
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

| Service   | URL                      |
|-----------|--------------------------|
| API       | http://localhost:8080    |
| Base path | `/api`                   |

Use the `local` profile to connect to Docker PostgreSQL and enable CORS for `http://localhost:3000`.

**Port conflict?** If 8080 is in use, start on another port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8081
```

Then set `NEXT_PUBLIC_API_URL` to match (see step 4).

### 4. Frontend

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

| Service  | URL                   |
|----------|------------------------|
| Frontend | http://localhost:3000  |

Ensure `frontend/.env.local` contains:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Running tests

```bash
# Backend (82 tests)
cd backend && ./mvnw test

# Frontend
cd frontend && npm test && npm run build
```

## API overview

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/tickets` | List tickets (`?search=`, `?status=`) |
| `POST` | `/api/tickets` | Create ticket |
| `GET` | `/api/tickets/{id}` | Get ticket with comments |
| `PATCH` | `/api/tickets/{id}` | Update fields (not status) |
| `PATCH` | `/api/tickets/{id}/status` | Status transition |
| `POST` | `/api/tickets/{id}/comments` | Add comment |

Full contract: [`spec/api-contract.md`](spec/api-contract.md).

## Project layout

```text
support-ticket-system/
├── backend/              # Spring Boot REST API
├── frontend/             # Next.js UI (App Router, TypeScript, Tailwind)
├── spec/                 # SDD specifications
├── docs/
│   ├── prompt-history.md
│   ├── e2e-run-log.md
│   ├── frontend-manual-checklist.md
│   └── known-limitations.md
├── docker-compose.yml
└── .env.example
```

## Documentation

| Document | Purpose |
|----------|---------|
| [spec/requirements.md](spec/requirements.md) | Requirements and acceptance criteria |
| [spec/architecture.md](spec/architecture.md) | System architecture |
| [spec/api-contract.md](spec/api-contract.md) | REST API contract |
| [spec/state-machine.md](spec/state-machine.md) | Ticket status transitions |
| [docs/prompt-history.md](docs/prompt-history.md) | AI interaction log |
| [docs/frontend-manual-checklist.md](docs/frontend-manual-checklist.md) | Manual UI checklist (M-01 – M-11) |
| [docs/e2e-run-log.md](docs/e2e-run-log.md) | E2E integration run log |
| [docs/known-limitations.md](docs/known-limitations.md) | Known limitations and deferred items |

## Known limitations

See [`docs/known-limitations.md`](docs/known-limitations.md) for security, search, CORS, pagination, and operational constraints.
