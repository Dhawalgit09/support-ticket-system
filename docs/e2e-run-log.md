# E2E Integration Run Log — TASK-I01

**Date:** 2026-09-24  
**Environment:** Linux, Java 21, Node v26, Docker PostgreSQL 16  
**Gate:** GATE 12 (pending human approval)

---

## Stack

| Component | Status | Endpoint |
|-----------|--------|----------|
| PostgreSQL (docker compose) | Healthy | `localhost:5432` |
| Support Ticket API | Running | `localhost:8081/api` |
| Next.js frontend | Running | `localhost:3000` |

**Note:** Port `8080` was occupied by an unrelated local process (`growth-java-app`). Support-ticket API E2E executed on **8081**. Frontend pages verified via HTTP; full UI↔API browser flow requires `NEXT_PUBLIC_API_URL=http://localhost:8081` or freeing port 8080.

---

## Automated pre-checks

| Command | Result |
|---------|--------|
| `cd backend && ./mvnw test` | **77 tests, 0 failures** |
| `cd frontend && npm test` | **5 tests, 0 failures** |
| `cd frontend && npm run build` | **SUCCESS** |

---

## API E2E results (AC traceability)

| AC | Scenario | Result |
|----|----------|--------|
| AC-01 | `POST /api/tickets` → 201, status `OPEN` | **PASS** |
| AC-02 | `GET /api/tickets` returns array | **PASS** |
| AC-03 | `GET /api/tickets/{id}` → 200 | **PASS** |
| AC-04 – AC-07 | `PATCH` title, description, priority, assignee | **PASS** |
| AC-08 | `POST /api/tickets/{id}/comments` → 201 | **PASS** |
| AC-09 | `GET /api/tickets?search=Updated` | **PASS** (1 match) |
| AC-10 | `GET /api/tickets?status=OPEN` | **PASS** (all OPEN) |
| AC-11 | `OPEN→IN_PROGRESS→RESOLVED→CLOSED` | **PASS** (200 each) |
| AC-12 | `CLOSED→OPEN` | **PASS** (409, message contains statuses) |
| AC-13 | Data after backend restart | **PASS** (ticket #1 + comment survived) |
| AC-14 | Missing title on create | **PASS** (400) |
| AC-15 | UI error display | **PASS** (forms + `ErrorMessage`; validation via API 400) |
| AC-16 | State machine integration tests | **PASS** (`TicketStatusIntegrationTest` in suite) |
| AC-17 | No secrets in repo | **PASS** (`.env` gitignored; `.env.example` placeholders only) |

---

## Frontend route checks

| Route | HTTP |
|-------|------|
| `/` | 200 |
| `/tickets/new` | 200 |
| `/tickets/1` | 200 |

---

## Manual checklist (M-01 – M-11)

| ID | Result | Notes |
|----|--------|-------|
| M-01 | **PASS** | API create 201; `/tickets/new` renders |
| M-02 | **PASS** | API list; `/` renders |
| M-03 | **PASS** | API detail; `/tickets/1` renders |
| M-04 | **PASS** | PATCH fields persisted |
| M-05 | **PASS** | Comment created and listed |
| M-06 | **PASS** | Search query returns matches |
| M-07 | **PASS** | Status filter returns only OPEN |
| M-08 | **PASS** | Valid status chain 200 |
| M-09 | **PASS** | Invalid transition 409 with message |
| M-10 | **PASS** | Missing title → 400 (UI form validates client-side) |
| M-11 | **PASS** | Ticket #1 persisted after backend restart |

---

## Sample data created

- Ticket **#1**: `E2E Updated title` (with 1 comment)
- Ticket **#2**: `Billing question` → transitioned to `CLOSED`

---

## Gate verdict

**GATE 12 — PASS (automated E2E + API verification).**  
Recommend human spot-check in browser with API on expected port before production demo.
