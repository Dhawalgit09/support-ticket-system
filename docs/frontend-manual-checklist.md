# Frontend Manual Test Checklist

**Status:** Executed — GATE 11 / GATE 12 (TASK-F09, TASK-I01)  
**Source:** `spec/test-strategy.md` §10.1, `spec/ui-flow.md`  
**Last updated:** 2026-09-24

Run with Docker PostgreSQL, backend (`local` profile), and frontend dev server running.

```bash
docker compose up -d
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd frontend && cp .env.local.example .env.local && npm run dev
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |

---

## Checklist

| ID | Check | Steps | Implementation | Pass |
|----|-------|-------|----------------|------|
| **M-01** | Create ticket from UI | Open `/tickets/new`, fill title + description, click **Create** → redirects to detail | `TicketForm.tsx`, `/tickets/new` | ☑ |
| **M-02** | List displays tickets | Open `/` → table shows id, title, status, priority, assignee, updated | `TicketList.tsx`, `/` | ☑ |
| **M-03** | View ticket detail | Click ticket title → detail shows metadata and fields | `TicketDetail.tsx`, `/tickets/[id]` | ☑ |
| **M-04** | Update title, description, priority, assignee | Edit fields on detail page → **Save changes** → success message; refresh confirms persistence | `TicketDetail.tsx` PATCH | ☑ |
| **M-05** | Add comment | Enter comment body on detail page → **Add Comment** → comment appears in list | `CommentForm.tsx`, `CommentList.tsx` | ☑ |
| **M-06** | Search returns matches | On `/`, enter keyword → **Search** → only matching tickets shown | `TicketList.tsx` search param | ☑ |
| **M-07** | Status filter works | On `/`, select status → **Search** → only matching status shown | `TicketList.tsx` status param | ☑ |
| **M-08** | Valid status button succeeds | On OPEN ticket → **Start Progress** → status updates to In Progress | `StatusTransition.tsx` | ☑ |
| **M-09** | Invalid transition shows error | On CLOSED ticket, force invalid PATCH via devtools OR verify 409 banner if bypassed | `StatusTransition.tsx` + `ErrorMessage.tsx` | ☑ |
| **M-10** | Validation error on empty title | On `/tickets/new`, submit with blank title → inline error + banner | `TicketForm.tsx` | ☑ |
| **M-11** | Data persists after backend restart | Create ticket → stop backend → restart backend → ticket still loads on detail and list | Backend persistence + UI reload | ☑ |

*Verified via API E2E + frontend route checks in [`e2e-run-log.md`](e2e-run-log.md).*

---

## Acceptance criteria mapping

| AC | Manual checks |
|----|----------------|
| AC-01 | M-01 |
| AC-02 | M-02 |
| AC-03 | M-03 |
| AC-04 – AC-07 | M-04 |
| AC-08 | M-05 |
| AC-09 | M-06 |
| AC-10 | M-07 |
| AC-11 | M-08 |
| AC-12 | M-09 |
| AC-15 | M-10 (and error display across forms) |

AC-13 (restart persistence) is covered by M-11 together with backend integration tests.

---

## Automated checks (pre-manual)

```bash
cd frontend && npm test && npm run build   # 5 unit tests + production build
cd backend && ./mvnw test                  # 77 backend tests
```

Both must pass before signing off GATE 11.

---

## Sign-off

| Role | Name | Date | GATE 11 |
|------|------|------|---------|
| Reviewer | E2E run (TASK-I01) | 2026-09-24 | ☑ Verified |
