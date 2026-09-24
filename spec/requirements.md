# Requirements

**Status:** Approved — GATE 2  
**Source:** `Assessments .md`  
**Last updated:** 2026-09-24

This document captures analysed requirements for the Support Ticket Management System. It is the source of truth for *what* must be built. Detailed *how* belongs in downstream spec artefacts (architecture, data model, API contract, etc.).

---

## 1. Document Conventions

| Label | Meaning |
|-------|---------|
| **REQ-** | Mandatory requirement from the assignment |
| **AC-** | Acceptance criterion — testable condition for completion |
| **DEC-** | Approved design decision — resolves ambiguity; binding for downstream specs |

---

## 2. Project Context

### 2.1 Purpose

Build a **Support Ticket Management System** that allows users to create, view, update, search, and filter support tickets, add comments, and transition tickets through a defined status lifecycle.

### 2.2 Assessment Context

The primary assessment goal is to demonstrate **Spec-Driven Development (SDD)** using AI tooling (Cursor / GitHub Copilot), not merely to deliver a working app. Requirements in Section 8 cover SDD process deliverables.

### 2.3 Out of Scope (unless added later)

The assignment does **not** require:

- User authentication or authorization
- Role-based access control
- Email or webhook notifications
- File attachments on tickets or comments
- Ticket deletion
- Audit logging beyond what persistence implies
- Multi-tenancy
- Real-time updates (WebSockets)

These are **not** requirements unless explicitly added and approved via a spec change.

---

## 3. Technology Requirements

| ID | Requirement |
|----|-------------|
| **REQ-TECH-01** | Backend must use **Java 21**. |
| **REQ-TECH-02** | Backend must use **Spring Boot**. |
| **REQ-TECH-03** | Backend must expose a **REST API**. |
| **REQ-TECH-04** | **PostgreSQL** must be the primary database for runtime/persistence. |
| **REQ-TECH-05** | **H2** may be used for testing and/or local development where appropriate. |
| **REQ-TECH-06** | Frontend must use **React**, **Next.js**, or an equivalent modern React-based framework. |
| **REQ-TECH-07** | Development must use **Cursor** (and GitHub Copilot may be used). |
| **REQ-TECH-08** | No secrets (credentials, API keys, tokens) must be committed to the repository. |

---

## 4. Functional Requirements — Tickets

| ID | Requirement | Source |
|----|-------------|--------|
| **REQ-FUNC-01** | The system must allow a user to **create a ticket**. | Assignment §Application Requirements #1 |
| **REQ-FUNC-02** | The system must allow a user to **list tickets**. | Assignment §Application Requirements #2 |
| **REQ-FUNC-03** | The system must allow a user to **view ticket details** (single ticket). | Assignment §Application Requirements #3 |
| **REQ-FUNC-04** | The system must allow a user to **update a ticket's title**. | Assignment §Application Requirements #4 |
| **REQ-FUNC-05** | The system must allow a user to **update a ticket's description**. | Assignment §Application Requirements #4 |
| **REQ-FUNC-06** | The system must allow a user to **update a ticket's priority**. | Assignment §Application Requirements #4 |
| **REQ-FUNC-07** | The system must allow a user to **update a ticket's assignee**. | Assignment §Application Requirements #4 |
| **REQ-FUNC-08** | The system must allow a user to **add comments** to a ticket. | Assignment §Application Requirements #5 |
| **REQ-FUNC-09** | The system must support **searching tickets by keyword**. | Assignment §Application Requirements #6 |
| **REQ-FUNC-10** | The system must support **filtering tickets by status**. | Assignment §Application Requirements #7 |
| **REQ-FUNC-11** | Ticket data must be **persisted in a database** and survive application restart. | Assignment §Application Requirements #8 |
| **REQ-FUNC-12** | The backend must **validate input** and reject invalid requests. | Assignment §Application Requirements #9 |
| **REQ-FUNC-13** | The UI must **display meaningful errors** when operations fail (validation, business rules, not found, etc.). | Assignment §Application Requirements #10 |

### 4.1 Minimum Ticket Data (assignment-implied)

The assignment requires create, view, update (title, description, priority, assignee), search, filter by status, and comments. At minimum, a ticket must support:

| Field | Required for REQ | Notes |
|-------|------------------|-------|
| Title | REQ-FUNC-01, 04 | Required; 1–200 characters — see DEC-08 |
| Description | REQ-FUNC-01, 05 | Required on create; max 5000 characters — see DEC-08 |
| Priority | REQ-FUNC-01, 06 | `LOW`, `MEDIUM`, `HIGH` — see DEC-01 |
| Assignee | REQ-FUNC-01, 07 | Optional free-text; max 100 characters — see DEC-02 |
| Status | REQ-FUNC-10, state machine | Lifecycle — see Section 5; new tickets `OPEN` — see DEC-03 |
| Identifier | REQ-FUNC-02, 03 | System-generated numeric ID |
| Timestamps | REQ-FUNC-11 | `createdAt`, `updatedAt` — see DEC-04 |

Comments are a separate concern (REQ-FUNC-08) but are associated with a ticket.

---

## 5. Functional Requirements — Status State Machine

The backend **must enforce** the following ticket status lifecycle. Invalid transitions **must be rejected** by the backend.

### 5.1 Statuses

| Status | Description (assignment-implied) |
|--------|----------------------------------|
| `OPEN` | Ticket created; not yet being worked |
| `IN_PROGRESS` | Ticket is actively being worked |
| `RESOLVED` | Work completed; pending closure |
| `CLOSED` | Ticket closed |
| `CANCELLED` | Ticket cancelled |

### 5.2 Valid Transitions

| ID | From | To |
|----|------|-----|
| **REQ-SM-01** | `OPEN` | `IN_PROGRESS` |
| **REQ-SM-02** | `IN_PROGRESS` | `RESOLVED` |
| **REQ-SM-03** | `RESOLVED` | `CLOSED` |
| **REQ-SM-04** | `OPEN` | `CANCELLED` |
| **REQ-SM-05** | `IN_PROGRESS` | `CANCELLED` |

### 5.3 Invalid Transitions (explicit assignment examples)

The backend **must reject** at least these transitions:

| ID | From | To |
|----|------|-----|
| **REQ-SM-06** | `CLOSED` | `OPEN` |
| **REQ-SM-07** | `RESOLVED` | `OPEN` |
| **REQ-SM-08** | `CANCELLED` | `OPEN` |

### 5.4 General Invalid-Transition Rule

| ID | Requirement |
|----|-------------|
| **REQ-SM-09** | Any status transition not listed in Section 5.2 must be **rejected** by the backend unless explicitly added via approved spec change. |

> **Note:** The assignment gives three invalid examples but states "Invalid transitions must be rejected" generally. Section 5.2 is the complete list of *valid* transitions. All others are invalid per REQ-SM-09.

### 5.5 Status Change Mechanism

| ID | Requirement |
|----|-------------|
| **REQ-SM-10** | Status changes must be enforced **server-side** (not UI-only). |
| **REQ-SM-11** | Invalid status transitions must return an error response suitable for display in the UI (see REQ-FUNC-13). HTTP status code to be defined in `spec/api-contract.md`. |

---

## 6. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| **REQ-NFR-01** | Data must survive application restart (persistent storage, not in-memory only). |
| **REQ-NFR-02** | Backend validation errors must be structured so the frontend can display field-level or message-level errors. |
| **REQ-NFR-03** | The solution must be suitable for demonstration and further development (readable structure, not throwaway prototype). |
| **REQ-NFR-04** | State-machine behaviour must be covered by **integration tests** (assignment acceptance criteria). |

---

## 7. Acceptance Criteria

Mapped from assignment **Core Acceptance Criteria**. Each criterion must be verifiable via manual test, automated test, or both.

| ID | Criterion | Traces to |
|----|-----------|-----------|
| **AC-01** | A ticket can be created from the UI. | REQ-FUNC-01, REQ-TECH-06 |
| **AC-02** | Tickets can be listed from the UI. | REQ-FUNC-02 |
| **AC-03** | Ticket details can be viewed from the UI. | REQ-FUNC-03 |
| **AC-04** | Ticket title can be updated. | REQ-FUNC-04 |
| **AC-05** | Ticket description can be updated. | REQ-FUNC-05 |
| **AC-06** | Ticket priority can be updated. | REQ-FUNC-06 |
| **AC-07** | Ticket assignee can be changed. | REQ-FUNC-07 |
| **AC-08** | Comments can be added to a ticket. | REQ-FUNC-08 |
| **AC-09** | Keyword search returns relevant tickets. | REQ-FUNC-09 |
| **AC-10** | Status filter returns only tickets matching the selected status. | REQ-FUNC-10 |
| **AC-11** | Valid status transitions succeed end-to-end (UI → API → persistence). | REQ-SM-01 – REQ-SM-05 |
| **AC-12** | Invalid status transitions are rejected by the backend (including assignment examples and REQ-SM-09). | REQ-SM-06 – REQ-SM-09 |
| **AC-13** | Data survives application restart. | REQ-FUNC-11, REQ-NFR-01 |
| **AC-14** | Backend validation rejects invalid input with appropriate error responses. | REQ-FUNC-12 |
| **AC-15** | UI displays meaningful errors for validation and business-rule failures. | REQ-FUNC-13 |
| **AC-16** | State-machine integration tests pass. | REQ-NFR-04, REQ-SM-10 |
| **AC-17** | No secrets are committed to the repository. | REQ-TECH-08 |

---

## 8. SDD Process Requirements (Assessment)

These requirements support the assessment of Spec-Driven Development practice. They are **not** application features.

| ID | Requirement | Status |
|----|-------------|--------|
| **REQ-SDD-01** | Maintain reusable AI instructions (rules, commands, skills). | Done (GATE 1) |
| **REQ-SDD-02** | Maintain specification artefacts before implementation. | In progress |
| **REQ-SDD-03** | Maintain prompt history (`docs/prompt-history.md` and `.specstory/history/`). | In progress |
| **REQ-SDD-04** | Follow workflow: Requirement → Specification → Plan → Implementation → Testing → Review → Fix. | Ongoing |
| **REQ-SDD-05** | Document at least one meaningful AI mistake and correction during the project. | See `docs/prompt-history.md` Entry 3 |

---

## 9. Approved Design Decisions

Approved at GATE 2 per human instruction: *resolve ambiguities with sensible defaults aligned to assignment requirements.* These decisions are **binding** for GATE 3–8 and implementation.

| ID | Decision | Rationale | Former ref |
|----|----------|-----------|------------|
| **DEC-01** | Priority enum values are **`LOW`**, **`MEDIUM`**, **`HIGH`**. Default on create: **`MEDIUM`**. | Standard support-ticket model; simple UI dropdown. | OQ-01 |
| **DEC-02** | Assignee is an **optional free-text string** (name or email as text). No user directory or auth. Max 100 characters. `null`/empty = unassigned. | Avoids user-management scope; satisfies assignee change requirement without auth. | OQ-02 |
| **DEC-03** | New tickets are created with status **`OPEN`**. Status cannot be set arbitrarily on create. | Only valid entry point for the state machine. | ASM-02 |
| **DEC-04** | Tickets have **`createdAt`** and **`updatedAt`** timestamps (UTC, ISO-8601 in API). List default sort: **`updatedAt` descending**. | Supports ordering and audit-friendly display. | ASM-01 |
| **DEC-05** | Comments are **append-only** (create only; no edit or delete). | Assignment requires add only; keeps scope minimal. | ASM-03 |
| **DEC-06** | **No authentication** — single logical operator; no login screen. | Not in assignment; reduces complexity. | ASM-04 |
| **DEC-07** | **Search** and **status filter** are combinable on the ticket list (`?search=` + `?status=`). | Natural list UX; satisfies both REQ-FUNC-09 and REQ-FUNC-10. | ASM-05 |
| **DEC-08** | **Validation constraints:** title required, 1–200 chars; description required on create, max 5000 chars; priority required (enum); assignee optional, max 100 chars; comment body required, max 2000 chars; comment author optional, max 100 chars. | Backend validation requirement; reasonable production limits. | OQ-08, OQ-09 |
| **DEC-09** | Keyword **search** matches **title and description** (not comments). Search is **case-insensitive**, substring match. | Covers primary ticket content; predictable behaviour for demos. | OQ-03, OQ-04 |
| **DEC-10** | Status changes use a **dedicated API action** separate from title/description/priority/assignee updates. Field PATCH must **not** change status. | Clear separation of business rules; simplifies state-machine enforcement. | OQ-05 |
| **DEC-11** | Frontend: **Next.js** (App Router) with **TypeScript** and React. | Explicitly listed in assignment; modern React stack. | OQ-06 |
| **DEC-12** | Ticket list is **unpaginated** for this exercise (return all matching tickets). | Sufficient for demo/assessment scope; avoids premature pagination design. | OQ-07 |
| **DEC-13** | List view displays at minimum: **id**, **title**, **status**, **priority**, **assignee**, **updatedAt**. | Identifies tickets and supports filter/search workflows. | ASM-06 |
| **DEC-14** | **`RESOLVED` → `IN_PROGRESS`** (reopen) and all transitions not in Section 5.2 are **invalid**. | Consistent with REQ-SM-09 and assignment examples. | OQ-10 |
| **DEC-15** | Backend build tool: **Maven**. Database migrations: **Flyway**. | Common Spring Boot defaults; details in GATE 3 architecture. | — |
| **DEC-16** | Monorepo layout: **`backend/`** (Spring Boot) and **`frontend/`** (Next.js) at repository root. | Clear separation for incremental SDD implementation. | — |

### 9.1 Comment Data (DEC-05, DEC-08)

| Field | Constraint |
|-------|------------|
| `body` | Required; 1–2000 characters |
| `author` | Optional free-text; max 100 characters |
| `createdAt` | System-set on create |

---

## 10. Requirements Traceability Matrix

| Assignment feature | Requirement IDs | Acceptance criteria |
|--------------------|-----------------|---------------------|
| Create a ticket | REQ-FUNC-01 | AC-01 |
| List tickets | REQ-FUNC-02 | AC-02 |
| View ticket details | REQ-FUNC-03 | AC-03 |
| Update title, description, priority, assignee | REQ-FUNC-04 – 07 | AC-04 – AC-07 |
| Add comments | REQ-FUNC-08 | AC-08 |
| Search by keyword | REQ-FUNC-09 | AC-09 |
| Filter by status | REQ-FUNC-10 | AC-10 |
| Persist in database | REQ-FUNC-11 | AC-13 |
| Validate input at backend | REQ-FUNC-12 | AC-14 |
| Display meaningful errors in UI | REQ-FUNC-13 | AC-15 |
| State machine | REQ-SM-01 – REQ-SM-11 | AC-11, AC-12, AC-16 |
| Java 21 / Spring Boot / REST / PostgreSQL / H2 | REQ-TECH-01 – 05 | Implicit in AC-01 – AC-16 |
| React/Next.js frontend | REQ-TECH-06 | AC-01 – AC-15 |
| No secrets committed | REQ-TECH-08 | AC-17 |

---

## 11. Gate Approval

| Gate | Document | Status |
|------|----------|--------|
| GATE 1 | Project structure & AI instructions | **Approved** |
| GATE 2 | This document (`spec/requirements.md`) | **Approved** (2026-09-24) |
| GATE 3 | `spec/architecture.md` | **Approved** (2026-09-24) |
| GATE 4 | `spec/data-model.md` | **Approved** (2026-09-24) |
| GATE 5 | `spec/api-contract.md` | **Approved** (2026-09-24) |
| GATE 6 | `spec/state-machine.md` | **Approved** (2026-09-24) |
| GATE 7 | `spec/ui-flow.md` | **Approved** (2026-09-24) |
| GATE 8 | `spec/test-strategy.md` | **Approved** (2026-09-24) |
| GATE 9 | `spec/implementation-plan.md` | **Approved** (2026-09-24) |

---

## 12. Revision History

| Date | Change | Gate |
|------|--------|------|
| 2026-09-24 | Initial requirements draft from `Assessments .md` | GATE 2 |
| 2026-09-24 | GATE 2 approved; ASM/OQ resolved as DEC-01 – DEC-16 | GATE 2 |
