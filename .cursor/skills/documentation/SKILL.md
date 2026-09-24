---
name: documentation
description: Create and maintain Support Ticket System documentation from approved specifications. Use when writing or updating spec files, architecture decisions, API docs, or prompt-history entries.
---

# Documentation Skill

## Source of Truth

Approved files under `spec/` are authoritative for system behavior.

Order of precedence:
1. `spec/requirements.md` (what we must build)
2. Other spec files (how we build it)
3. `.cursor/rules/` and `.cursor/commands/` (how we implement and review)
4. Implementation code (must match specs)

## Document Types

| Type | Location | Content |
|------|----------|---------|
| Requirements | `spec/requirements.md` | Features, acceptance criteria |
| Architecture | `spec/architecture.md` | Stack, layers, deployment |
| Data model | `spec/data-model.md` | Entities, fields, relationships |
| API contract | `spec/api-contract.md` | Endpoints, payloads, errors |
| State machine | `spec/state-machine.md` | Statuses and transitions |
| UI flow | `spec/ui-flow.md` | Screens, actions, error display |
| Test strategy | `spec/test-strategy.md` | What to test and how |
| Decisions | `docs/prompt-history.md` | Prompts, outcomes, corrections |
| AI trace | `.specstory/history/` | SpecStory captures (if extension enabled) |
| Project rules | `.cursor/rules/` | Java, testing, API standards |
| Project commands | `.cursor/commands/` | Review and test-generation workflows |

## Writing Rules

- **Label clearly**: Requirement vs Decision vs Assumption vs Implementation detail.
- **Do not document unimplemented behavior** as if it exists.
- **Use consistent terminology** — same status names and field names across all docs.
- **Include examples** for API payloads, transitions, and UI errors when it reduces ambiguity.
- **Record spec changes**: date, what changed, why, and which gate approved it.

## When Updating Documentation

1. Check if the change is a requirement change — if so, update `requirements.md` first and get approval.
2. Update affected spec files (data-model, api-contract, etc.).
3. Run mental `.cursor/commands/review-spec.md` checklist before requesting gate approval.
4. Add entry to `docs/prompt-history.md` for significant AI-assisted edits.

## Anti-Patterns

- Copying code into docs instead of describing contract behavior.
- Drift between `spec/api-contract.md` and actual controllers.
- Undocumented assumptions (e.g. assignee format).
- Fabricated prompt history entries.
