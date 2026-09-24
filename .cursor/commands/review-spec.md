# Command: Review Specification

Review specification document(s) for completeness, consistency, and implementability.

## Inputs

1. `Assessments .md` or `spec/requirements.md` (source requirements)
2. Target spec file(s): architecture, data-model, api-contract, state-machine, ui-flow, test-strategy
3. Other spec files for cross-reference

## Review Checklist

- [ ] All assignment requirements traced to spec sections
- [ ] No contradictions between spec documents
- [ ] Terminology consistent (status names, field names, enum values)
- [ ] Acceptance criteria testable and unambiguous
- [ ] Error cases documented (validation, not found, conflict)
- [ ] All state transitions listed (valid and invalid examples)
- [ ] API behavior defined: methods, paths, request/response, status codes
- [ ] Persistence behavior: entities, relationships, indexes, migrations
- [ ] Search/filter semantics defined (which fields, case sensitivity)
- [ ] UI flows cover create, list, detail, update, comment, search, filter, errors
- [ ] Test strategy covers state machine, validation, persistence, API
- [ ] Assumptions explicitly labeled as assumptions (not requirements)
- [ ] Open questions listed where human decision is needed

## Output Format

### Summary
Brief assessment: ready for approval / needs revision.

### Gaps
Missing requirements or unspecified behavior.

### Contradictions
Conflicts between documents with references.

### Ambiguities
Items requiring human decision before implementation.

### Recommendations
Concrete edits to spec files (do not implement code).

## Rules

- Do not add new business requirements without labeling them as proposals.
- Prefer evidence from `Assessments .md` over invented features.
- Invalid state transitions must be explicit, not implied.
