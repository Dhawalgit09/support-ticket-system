# Test Strategy

**Status:** Approved — GATE 8  
**Source:** `spec/requirements.md`, `spec/architecture.md`, `spec/api-contract.md`, `spec/state-machine.md`, `spec/ui-flow.md`, `.cursor/rules/testing.mdc`  
**Last updated:** 2026-09-24

Defines what to test, how to test it, tooling, and coverage expectations for the Support Ticket Management System.

---

## 1. Goals

| Goal | Requirement trace |
|------|-------------------|
| Prove acceptance criteria with automated tests where practical | AC-01 – AC-17 |
| Mandatory state-machine integration coverage | REQ-NFR-04, AC-16 |
| Backend validation and error responses verified | REQ-FUNC-12, AC-14 |
| Persistence survives context restart | REQ-NFR-01, AC-13 |
| Test behaviour, not implementation details | `.cursor/rules/testing.mdc` |

---

## 2. Test Pyramid

```text
        ┌─────────────┐
        │  API / MVC  │  Fewer — HTTP contract, status codes, JSON shape
        ├─────────────┤
        │ Integration │  State machine, persistence, search/filter
        ├─────────────┤
        │    Unit     │  TicketStatusService, business rules (mocked repos)
        └─────────────┘
```

| Layer | Primary focus | Database |
|-------|---------------|----------|
| **Unit** | `TicketStatusService`, pure business logic | Mocked |
| **Integration** | Full Spring context + real DB operations | H2 (`test` profile) |
| **API (MockMvc)** | Controller mapping, validation, error envelopes | Mocked services or `@SpringBootTest` |

**Frontend:** Manual acceptance testing is the **minimum** for assessment. Automated frontend tests are **optional** (Section 10).

---

## 3. Tooling

### 3.1 Backend (Maven dependencies at implementation)

| Tool | Purpose |
|------|---------|
| **JUnit 5** | Test framework |
| **Mockito** | Mock dependencies in unit tests |
| **AssertJ** | Fluent assertions |
| **Spring Boot Test** | `@SpringBootTest`, `@WebMvcTest`, `MockMvc` |
| **H2** | In-memory database for integration tests (REQ-TECH-05) |
| **Flyway** | Apply migrations in test profile for schema parity |

### 3.2 Commands

```bash
cd backend && ./mvnw test          # All backend tests
cd backend && ./mvnw verify        # If integration phase added later
```

### 3.3 Test Profile

| Setting | Value |
|---------|-------|
| Active profile | `test` |
| Datasource | H2 in-memory |
| Flyway | Enabled — run `db/migration` scripts |
| `ddl-auto` | `validate` (prefer Flyway-managed schema) |

Configuration: `application-test.yml` per `spec/architecture.md`.

---

## 4. Test Organization

```text
backend/src/test/java/com/supportticket/
├── service/
│   ├── TicketStatusServiceTest.java       # Unit
│   ├── TicketServiceTest.java             # Unit
│   └── CommentServiceTest.java            # Unit
├── integration/
│   ├── TicketStatusIntegrationTest.java   # State machine (mandatory)
│   ├── TicketPersistenceIntegrationTest.java
│   ├── TicketSearchIntegrationTest.java
│   └── CommentIntegrationTest.java
├── controller/
│   ├── TicketControllerTest.java          # MockMvc
│   └── CommentControllerTest.java         # MockMvc
└── repository/
    └── TicketRepositoryTest.java          # Optional if covered by integration
```

Class names are illustrative; package structure should match `spec/architecture.md`.

---

## 5. Naming Convention

```
methodName_condition_expectedOutcome
```

Examples:

- `createTicket_withValidBody_returns201AndOpenStatus`
- `transitionStatus_fromClosedToOpen_returns409AndUnchangedStatus`
- `listTickets_withStatusFilter_returnsOnlyMatchingTickets`
- `searchTickets_caseInsensitive_matchesTitleOrDescription`

---

## 6. Unit Tests

### 6.1 `TicketStatusServiceTest` (priority)

| Test | Assert |
|------|--------|
| Each valid transition (SM-VT-01 – 05) | Returns success; correct target status |
| Each notable invalid transition | Throws or returns failure; status unchanged |
| Same-state transition | Rejected |
| Transition from terminal state | Rejected |

Mock `TicketRepository` — verify `save` called only on success.

### 6.2 `TicketServiceTest`

| Test | Assert |
|------|--------|
| Create ticket | Default `OPEN`, `MEDIUM` priority, timestamps set |
| Update fields | Only provided fields change; `updatedAt` bumped |
| Update with `status` in request | Rejected before persistence |
| Get by id not found | Throws `TicketNotFoundException` (or equivalent) |

### 6.3 `CommentServiceTest`

| Test | Assert |
|------|--------|
| Add comment to existing ticket | Comment saved; ticket `updatedAt` bumped (DM-01) |
| Add comment to missing ticket | Not found |

---

## 7. Integration Tests

Use `@SpringBootTest` + `MockMvc` or `TestRestTemplate` with `test` profile and H2.

### 7.1 State Machine — `TicketStatusIntegrationTest` (MANDATORY)

Implements all cases from `spec/state-machine.md` §9:

| ID | Test | HTTP | Assert |
|----|------|------|--------|
| SM-IT-01 | `OPEN` → `IN_PROGRESS` | 200 | DB status updated |
| SM-IT-02 | `OPEN` → `CANCELLED` | 200 | DB status updated |
| SM-IT-03 | `IN_PROGRESS` → `RESOLVED` | 200 | DB status updated |
| SM-IT-04 | `IN_PROGRESS` → `CANCELLED` | 200 | DB status updated |
| SM-IT-05 | `RESOLVED` → `CLOSED` | 200 | DB status updated |
| SM-IT-06 | Full chain on one ticket | 200 × 3 | Final status `CLOSED` |
| SM-IT-07 | `CLOSED` → `OPEN` | 409 | Status unchanged in DB |
| SM-IT-08 | `RESOLVED` → `OPEN` | 409 | Status unchanged |
| SM-IT-09 | `CANCELLED` → `OPEN` | 409 | Status unchanged |
| SM-IT-10 | `RESOLVED` → `IN_PROGRESS` | 409 | Status unchanged |
| SM-IT-11 | `OPEN` → `RESOLVED` | 409 | Status unchanged |
| SM-IT-12 | `OPEN` → `OPEN` | 409 | Status unchanged |
| SM-IT-13 | `CLOSED` → `CANCELLED` | 409 | Status unchanged |
| SM-IT-14 | `status` on field PATCH | 400 | Message references status endpoint |
| SM-IT-15 | POST create ticket | 201 | Response + DB status `OPEN` |

**Invalid transition assertions (SM-IT-07 – 13):**

- HTTP **409**
- `error` = `Invalid Status Transition`
- `message` contains `from` and `to` statuses
- Reload entity from DB — status unchanged

### 7.2 Persistence — `TicketPersistenceIntegrationTest`

| Test | Assert |
|------|--------|
| Create and read ticket | Fields round-trip correctly |
| Data survives restart | Create ticket → new `@SpringBootTest` context or clear EM + re-read → data still present (AC-13) |
| Update persists | PATCH then GET returns new values |

*Restart simulation:* use same H2 in-memory URL with `DB_CLOSE_DELAY=-1` or `@DirtiesContext` with re-fetch from repository after flush.

### 7.3 Search & Filter — `TicketSearchIntegrationTest`

| Test | Assert |
|------|--------|
| Search matches title | DEC-09 |
| Search matches description | DEC-09 |
| Search is case-insensitive | DEC-09 |
| Filter by status | REQ-FUNC-10 |
| Combined search + status | DEC-07 |
| No matches | Empty array |

### 7.4 Comments — `CommentIntegrationTest`

| Test | Assert |
|------|--------|
| Add comment | 201; comment in GET detail |
| Add comment updates ticket `updatedAt` | DM-01 |
| Comment on missing ticket | 404 |
| Validation — empty body | 400 |

---

## 8. API / Controller Tests (MockMvc)

Can overlap with integration tests; avoid duplicate coverage **unless** testing different layers. Prefer integration tests for state machine; use MockMvc for focused validation tests.

### 8.1 `TicketControllerTest`

| Test | Assert |
|------|--------|
| POST valid ticket | 201, `Location` header, body shape |
| POST missing title | 400, `fieldErrors` |
| POST title too long | 400 |
| GET list | 200, array shape |
| GET by id | 200 / 404 |
| PATCH valid update | 200 |
| PATCH with status field | 400 |
| GET invalid status query param | 400 |

### 8.2 `CommentControllerTest`

| Test | Assert |
|------|--------|
| POST valid comment | 201 |
| POST empty body | 400 |
| POST on missing ticket | 404 |

### 8.3 Error Response Shape

At least one test per error type verifies JSON structure per `spec/api-contract.md` §3:

- `timestamp`, `status`, `error`, `message`, `path` present
- `fieldErrors` array for 400 validation

---

## 9. Repository Tests (Optional)

Add `TicketRepositoryTest` **only if** search/filter queries are not fully exercised by `TicketSearchIntegrationTest`.

If added:

- `@DataJpaTest` with H2
- Test custom `@Query` for search and status filter

---

## 10. Frontend Testing

### 10.1 Minimum (Assessment)

**Manual test checklist** derived from `spec/ui-flow.md` and acceptance criteria:

| # | Manual check |
|---|--------------|
| M-01 | Create ticket from UI |
| M-02 | List displays tickets |
| M-03 | View ticket detail |
| M-04 | Update title, description, priority, assignee |
| M-05 | Add comment |
| M-06 | Search returns matches |
| M-07 | Status filter works |
| M-08 | Valid status button succeeds |
| M-09 | Invalid transition shows error message (if testable via devtools or forced bad request) |
| M-10 | Validation error displayed on empty title |
| M-11 | Data persists after backend restart |

Record manual test execution in PR description or `docs/` at implementation time.

### 10.2 Optional Automated Frontend Tests

Not required for GATE 8 approval. If added:

- **Playwright** or **Cypress** for E2E against running backend + frontend
- Scope: one happy-path flow (create → transition → comment)

---

## 11. Validation Test Matrix

| Scenario | Endpoint | Expected |
|----------|----------|----------|
| Blank title | POST / PATCH | 400 |
| Title > 200 chars | POST / PATCH | 400 |
| Blank description on create | POST | 400 |
| Description > 5000 chars | POST / PATCH | 400 |
| Invalid priority enum | POST / PATCH | 400 |
| Assignee > 100 chars | POST / PATCH | 400 |
| Blank comment body | POST comments | 400 |
| Invalid status enum in transition | PATCH status | 400 |
| Missing ticket id | GET / PATCH | 404 |

---

## 12. What Not to Test

Per `.cursor/rules/testing.mdc`:

- Private methods in isolation
- Spring Framework internals (e.g. that `@Valid` annotation exists without HTTP call)
- JPA / Hibernate behaviour unrelated to project queries
- Generated getters/setters without logic
- Duplicate identical assertions in unit + integration for the same behaviour (pick the more valuable layer)

---

## 13. Coverage Expectations

| Area | Expectation |
|------|-------------|
| State machine | **100%** of valid + required invalid cases (SM-IT-01 – 15) |
| API endpoints | Each endpoint has ≥1 happy-path + ≥1 error test |
| Validation | Key constraints from DEC-08 covered |
| Search/filter | At least one test per capability |
| Frontend | Manual checklist (Section 10.1) |
| Line coverage % | No fixed target — quality over percentage |

---

## 14. Acceptance Criteria Traceability

| AC | Automated test reference |
|----|--------------------------|
| AC-01 | M-01; `TicketControllerTest` POST 201 |
| AC-02 | M-02; list integration test |
| AC-03 | GET detail integration / MockMvc |
| AC-04 – AC-07 | PATCH integration tests |
| AC-08 | Comment integration test |
| AC-09 | `TicketSearchIntegrationTest` |
| AC-10 | Status filter integration test |
| AC-11 | SM-IT-01 – 06 |
| AC-12 | SM-IT-07 – 13 |
| AC-13 | `TicketPersistenceIntegrationTest` |
| AC-14 | Validation matrix §11 |
| AC-15 | Manual M-10 (UI) |
| AC-16 | Full `TicketStatusIntegrationTest` suite |
| AC-17 | Process check — no secrets in repo (not a JUnit test) |

---

## 15. Test Execution in SDD Workflow

| Phase | Action |
|-------|--------|
| Implementation | Write tests alongside feature per `.cursor/commands/generate-tests.md` |
| Before PR | `./mvnw test` must pass |
| Review | Run `.cursor/commands/review-code.md` including test adequacy |
| CI (optional) | GitHub Actions `mvn test` on push — not required for assessment |

---

## 16. Out of Scope

- Performance / load testing
- Security penetration testing
- Contract testing (Pact) between frontend and backend
- Mutation testing
- Testcontainers PostgreSQL (H2 sufficient for assessment; PostgreSQL verified manually)
- Visual regression testing

---

## 17. Gate Approval

| Gate | Document | Status |
|------|----------|--------|
| GATE 1 – 7 | Prior specs | **Approved** |
| GATE 8 | This document (`spec/test-strategy.md`) | **Approved** (2026-09-24) |
| GATE 9 | Implementation plan | Not started |

---

## 18. Revision History

| Date | Change | Gate |
|------|--------|------|
| 2026-09-24 | Initial test strategy draft | GATE 8 |
| 2026-09-24 | GATE 8 approved | GATE 8 |
