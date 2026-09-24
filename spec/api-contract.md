# API Contract

**Status:** Approved — GATE 5  
**Source:** `spec/requirements.md`, `spec/data-model.md`, `spec/architecture.md`, `.cursor/rules/api-standards.mdc`  
**Last updated:** 2026-09-24

REST API contract for the Support Ticket Management System. Base URL: `http://localhost:8080` (local). All paths are prefixed with `/api`.

---

## 1. Conventions

| Topic | Rule |
|-------|------|
| Base path | `/api` |
| Versioning | None (no `/v1` prefix) |
| Content-Type | `application/json` for request and response bodies |
| Charset | UTF-8 |
| Date/time | ISO-8601 UTC strings, e.g. `"2026-09-24T14:30:00Z"` |
| Enum values | `UPPER_SNAKE` strings |
| Null assignee | JSON `null` or omitted in response = unassigned |
| Pagination | **None** — list endpoints return a JSON array (DEC-12) |

---

## 2. Shared Types

### 2.1 `TicketStatus`

`OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`

### 2.2 `TicketPriority`

`LOW` | `MEDIUM` | `HIGH`

### 2.3 `TicketSummaryResponse`

Used in list responses (DEC-13).

```json
{
  "id": 1,
  "title": "Cannot login",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "alice@example.com",
  "updatedAt": "2026-09-24T14:30:00Z"
}
```

| Field | Type | Required in response |
|-------|------|----------------------|
| `id` | number | yes |
| `title` | string | yes |
| `status` | `TicketStatus` | yes |
| `priority` | `TicketPriority` | yes |
| `assignee` | string \| null | yes |
| `updatedAt` | string (ISO-8601) | yes |

### 2.4 `TicketDetailResponse`

```json
{
  "id": 1,
  "title": "Cannot login",
  "description": "User reports 500 error on login page.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "alice@example.com",
  "createdAt": "2026-09-24T10:00:00Z",
  "updatedAt": "2026-09-24T14:30:00Z",
  "comments": [
    {
      "id": 10,
      "body": "Investigating logs.",
      "author": "Bob",
      "createdAt": "2026-09-24T12:00:00Z"
    }
  ]
}
```

| Field | Type | Required in response |
|-------|------|----------------------|
| `id` | number | yes |
| `title` | string | yes |
| `description` | string | yes |
| `status` | `TicketStatus` | yes |
| `priority` | `TicketPriority` | yes |
| `assignee` | string \| null | yes |
| `createdAt` | string (ISO-8601) | yes |
| `updatedAt` | string (ISO-8601) | yes |
| `comments` | `CommentResponse[]` | yes (empty array if none) |

### 2.5 `CommentResponse`

```json
{
  "id": 10,
  "body": "Investigating logs.",
  "author": "Bob",
  "createdAt": "2026-09-24T12:00:00Z"
}
```

| Field | Type | Required in response |
|-------|------|----------------------|
| `id` | number | yes |
| `body` | string | yes |
| `author` | string \| null | yes |
| `createdAt` | string (ISO-8601) | yes |

---

## 3. Error Responses

All errors use a consistent envelope. No stack traces or internal class names.

### 3.1 Validation Error (400)

```json
{
  "timestamp": "2026-09-24T14:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/tickets",
  "fieldErrors": [
    {
      "field": "title",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

`fieldErrors` may be empty when the failure is not field-specific (e.g. malformed JSON).

### 3.2 Not Found (404)

```json
{
  "timestamp": "2026-09-24T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 42",
  "path": "/api/tickets/42"
}
```

### 3.3 Conflict — Invalid Status Transition (409)

```json
{
  "timestamp": "2026-09-24T14:30:00Z",
  "status": 409,
  "error": "Invalid Status Transition",
  "message": "Cannot transition from CLOSED to OPEN",
  "path": "/api/tickets/1/status"
}
```

No `fieldErrors` for 409 responses.

### 3.4 Business Rule — Status via Wrong Endpoint (400)

When `status` is sent on ticket field PATCH:

```json
{
  "timestamp": "2026-09-24T14:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Status cannot be updated via this endpoint. Use PATCH /api/tickets/{id}/status",
  "path": "/api/tickets/1"
}
```

### 3.5 Server Error (500)

```json
{
  "timestamp": "2026-09-24T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/tickets"
}
```

---

## 4. Endpoints

### 4.1 Create Ticket

**`POST /api/tickets`**

Creates a ticket with status `OPEN` (DEC-03). Client cannot set `status`.

#### Request: `CreateTicketRequest`

```json
{
  "title": "Cannot login",
  "description": "User reports 500 error on login page.",
  "priority": "HIGH",
  "assignee": "alice@example.com"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `title` | string | yes | Trim; 1–200 chars |
| `description` | string | yes | Trim; 1–5000 chars |
| `priority` | `TicketPriority` | no | Default `MEDIUM` if omitted |
| `assignee` | string | no | Trim; max 100 chars; empty → `null` |

#### Response

| Status | Body | Headers |
|--------|------|---------|
| **201 Created** | `TicketDetailResponse` | `Location: /api/tickets/{id}` |
| **400 Bad Request** | Validation error (§3.1) | — |

#### Example

```http
POST /api/tickets
Content-Type: application/json

{
  "title": "Cannot login",
  "description": "User reports 500 error on login page.",
  "priority": "HIGH"
}
```

---

### 4.2 List Tickets

**`GET /api/tickets`**

Returns all matching tickets sorted by `updatedAt` descending (DEC-04). Unpaginated array (DEC-12).

#### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `search` | string | no | Case-insensitive substring match on **title** and **description** (DEC-09) |
| `status` | `TicketStatus` | no | Exact status filter (DEC-07, REQ-FUNC-10) |

Parameters are combinable: `GET /api/tickets?search=login&status=OPEN`

| Combination | Behaviour |
|-------------|-----------|
| No params | All tickets, sorted by `updatedAt` desc |
| `search` only | Tickets matching keyword |
| `status` only | Tickets with given status |
| Both | Tickets matching keyword **and** status |

Empty `search` param is ignored (treated as no search). Invalid `status` enum → **400**.

#### Response

| Status | Body |
|--------|------|
| **200 OK** | `TicketSummaryResponse[]` |
| **400 Bad Request** | Validation error if `status` is invalid |

#### Example

```http
GET /api/tickets?search=login&status=OPEN
```

```json
[
  {
    "id": 1,
    "title": "Cannot login",
    "status": "OPEN",
    "priority": "HIGH",
    "assignee": null,
    "updatedAt": "2026-09-24T14:30:00Z"
  }
]
```

---

### 4.3 Get Ticket by ID

**`GET /api/tickets/{id}`**

Returns ticket detail including comments ordered by `createdAt` ascending.

#### Path Parameters

| Param | Type | Description |
|-------|------|-------------|
| `id` | number | Ticket ID |

#### Response

| Status | Body |
|--------|------|
| **200 OK** | `TicketDetailResponse` |
| **404 Not Found** | Error (§3.2) |

---

### 4.4 Update Ticket Fields

**`PATCH /api/tickets/{id}`**

Partial update of **title**, **description**, **priority**, and **assignee** only (DEC-10). **Must not** change `status`.

#### Request: `UpdateTicketRequest`

All fields optional; at least one field must be present.

```json
{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "LOW",
  "assignee": "bob@example.com"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `title` | string | no | If present: trim; 1–200 chars |
| `description` | string | no | If present: trim; 1–5000 chars |
| `priority` | `TicketPriority` | no | Valid enum |
| `assignee` | string \| null | no | Trim; max 100 chars; `null` or `""` clears assignee |
| `status` | — | **forbidden** | If present → **400** (§3.4) |

#### Response

| Status | Body |
|--------|------|
| **200 OK** | `TicketDetailResponse` |
| **400 Bad Request** | Validation error; empty body; `status` in body |
| **404 Not Found** | Error (§3.2) |

---

### 4.5 Update Ticket Status

**`PATCH /api/tickets/{id}/status`**

Dedicated status transition endpoint (DEC-10). Enforces state machine rules (REQ-SM-*). Full transition matrix: `spec/state-machine.md` (GATE 6).

#### Request: `UpdateTicketStatusRequest`

```json
{
  "status": "IN_PROGRESS"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `status` | `TicketStatus` | yes | Valid enum; transition must be allowed |

#### Response

| Status | Body |
|--------|------|
| **200 OK** | `TicketDetailResponse` |
| **400 Bad Request** | Missing/invalid `status` |
| **404 Not Found** | Ticket not found (§3.2) |
| **409 Conflict** | Invalid transition (§3.3) |

#### Example — valid transition

```http
PATCH /api/tickets/1/status
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

#### Example — invalid transition

```http
PATCH /api/tickets/1/status
{ "status": "OPEN" }
```

When current status is `CLOSED` → **409** with message `Cannot transition from CLOSED to OPEN`.

---

### 4.6 Add Comment

**`POST /api/tickets/{id}/comments`**

Appends a comment to a ticket (DEC-05). Updates parent ticket `updatedAt` (DM-01).

#### Request: `CreateCommentRequest`

```json
{
  "body": "Investigating logs.",
  "author": "Bob"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `body` | string | yes | Trim; 1–2000 chars |
| `author` | string | no | Trim; max 100 chars; empty → `null` |

#### Response

| Status | Body | Headers |
|--------|------|---------|
| **201 Created** | `CommentResponse` | `Location: /api/tickets/{ticketId}/comments/{commentId}` |
| **400 Bad Request** | Validation error |
| **404 Not Found** | Ticket not found |

---

## 5. Endpoint Summary

| Method | Path | Purpose | Success |
|--------|------|---------|---------|
| `POST` | `/api/tickets` | Create ticket | 201 |
| `GET` | `/api/tickets` | List / search / filter | 200 |
| `GET` | `/api/tickets/{id}` | Get ticket + comments | 200 |
| `PATCH` | `/api/tickets/{id}` | Update fields (not status) | 200 |
| `PATCH` | `/api/tickets/{id}/status` | Status transition | 200 |
| `POST` | `/api/tickets/{id}/comments` | Add comment | 201 |

No `DELETE` endpoints (out of scope).

---

## 6. HTTP Status Code Matrix

| Situation | Code |
|-----------|------|
| Successful read | 200 |
| Successful create | 201 |
| Validation failure | 400 |
| Ticket not found | 404 |
| Invalid status transition | 409 |
| Unexpected server error | 500 |

---

## 7. Validation Rules Summary

| Endpoint | Rules |
|----------|-------|
| Create ticket | `title`, `description` required; lengths per DEC-08; `priority` defaults to `MEDIUM` |
| Update ticket | At least one allowed field; reject `status` field |
| Update status | `status` required; transition validated server-side |
| Add comment | `body` required |
| List tickets | Invalid `status` query param → 400 |

---

## 8. Requirements Traceability

| Requirement / Decision | API element |
|------------------------|-------------|
| REQ-FUNC-01 | POST `/api/tickets` |
| REQ-FUNC-02 | GET `/api/tickets` |
| REQ-FUNC-03 | GET `/api/tickets/{id}` |
| REQ-FUNC-04 – 07 | PATCH `/api/tickets/{id}` |
| REQ-FUNC-08 | POST `/api/tickets/{id}/comments` |
| REQ-FUNC-09 | GET `/api/tickets?search=` |
| REQ-FUNC-10 | GET `/api/tickets?status=` |
| REQ-FUNC-12 | §3, §7 validation |
| REQ-FUNC-13 | Structured error responses §3 |
| DEC-07 | Combined `search` + `status` query params |
| DEC-09 | Search on title + description |
| DEC-10 | Separate PATCH `.../status`; reject status on field PATCH |
| DEC-12 | Unpaginated array response |
| DEC-13 | `TicketSummaryResponse` fields |

---

## 9. Out of Scope

- Authentication headers
- Rate limiting
- Pagination query params
- Bulk operations
- Ticket or comment deletion
- Webhooks / SSE
- OpenAPI document generation (optional at implementation)

---

## 10. Gate Approval

| Gate | Document | Status |
|------|----------|--------|
| GATE 1 – 4 | Prior specs | **Approved** |
| GATE 5 | This document (`spec/api-contract.md`) | **Approved** (2026-09-24) |
| GATE 6 | `spec/state-machine.md` | **Approved** (2026-09-24) |
| GATE 7 | `spec/ui-flow.md` | Not started |
| GATE 8 | `spec/test-strategy.md` | Not started |

---

## 11. Revision History

| Date | Change | Gate |
|------|--------|------|
| 2026-09-24 | Initial API contract draft | GATE 5 |
| 2026-09-24 | GATE 5 approved | GATE 5 |
