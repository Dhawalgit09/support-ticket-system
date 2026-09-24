# Command: Generate Tests

Generate tests from specifications and expected behavior — not from implementation shortcuts.

## Workflow

1. **Read the specification first**
   - Relevant sections of `spec/requirements.md`, `spec/api-contract.md`, `spec/state-machine.md`, `spec/data-model.md`, `spec/test-strategy.md`.

2. **Read the implementation second**
   - Identify classes and endpoints that implement the specified behavior.

3. **Generate tests** that prove spec compliance.

## Required Coverage

### Positive cases
- Happy path for each API operation defined in the contract.
- Valid state transitions per `spec/state-machine.md`.

### Negative cases
- Validation failures (blank title, invalid enum, field length).
- Invalid state transitions (expect 409 or status defined in contract).
- Not found (404).
- Malformed requests (400).

### State machine (mandatory integration tests)
- OPEN → IN_PROGRESS → RESOLVED → CLOSED (full chain)
- OPEN → CANCELLED
- IN_PROGRESS → CANCELLED
- Reject: CLOSED → OPEN, RESOLVED → OPEN, CANCELLED → OPEN, and any unspecified transition

## Test Style

- Follow `.cursor/rules/testing.mdc` naming and structure.
- Use meaningful assertions on response body and persisted state.
- Do not test private methods or framework internals.
- Do not duplicate identical tests across layers unless each layer adds distinct value per test strategy.

## Output

- List tests to create or implement with class name, method name, and spec reference.
- If spec is incomplete, **stop** and list missing spec items — do not guess behavior.
