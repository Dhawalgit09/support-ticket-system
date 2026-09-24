# Known Limitations

Documented constraints and deferred items for the Support Ticket System as of GATE 13 (2026-09-24).

## Security & Access

| Limitation | Notes |
|------------|-------|
| No authentication | All API endpoints are open. Suitable for local/training use only. |
| No authorization | Any client can read, update, or transition any ticket. |
| Default DB credentials | Docker Compose uses `support`/`support` — change for non-local deployments. |

## Backend

| Limitation | Notes |
|------------|-------|
| CORS on `local` profile only | Frontend dev server (`http://localhost:3000`) is allowed only when the backend runs with `-Dspring-boot.run.profiles=local`. |
| Search `LIKE` wildcards | Keyword search does not escape `%` or `_` in user input; special characters may broaden matches unexpectedly. |
| No pagination | `GET /api/tickets` returns the full result set per `spec/api-contract.md`. |
| No soft delete | Tickets are not deletable via API. |
| Assignee format | Free-text field; no email validation beyond length. |

## Frontend

| Limitation | Notes |
|------------|-------|
| Limited automated tests | API client unit tests only (`client.test.ts`); UI components rely on manual checklist M-01 – M-11. |
| No offline support | Requires a running backend; network errors show a generic banner. |
| Browser `confirm` replaced | Cancel transitions use an in-app dialog; other flows have no undo. |

## Operations

| Limitation | Notes |
|------------|-------|
| Port 8080 conflicts | If another process uses 8080, run the backend on an alternate port and set `NEXT_PUBLIC_API_URL` accordingly (see `docs/e2e-run-log.md`). |
| Single-node PostgreSQL | No replication, backup, or migration runbook beyond Flyway on startup. |
| No CI pipeline | Tests are run locally; no GitHub Actions or similar configured in this repo. |

## Deferred (documented in TASK-I02 review)

- Escape `%`/`_` in search queries
- Extract duplicated `toDetailResponse()` mapping in backend services
- Production CORS and profile configuration for deployed environments
