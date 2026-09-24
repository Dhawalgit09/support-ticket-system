# Command: Review Code

Review the selected or referenced code changes against approved project artifacts.

## Inputs

1. Read `spec/requirements.md`, `spec/architecture.md`, `spec/api-contract.md`, `spec/state-machine.md`, and `spec/data-model.md`.
2. Read `.cursor/rules/java-springboot.mdc`, `.cursor/rules/testing.mdc`, and `.cursor/rules/api-standards.mdc`.
3. Read the implementation under review (diff or files).

## Review Checklist

- [ ] Matches approved specifications (no silent requirement invention)
- [ ] Follows layered architecture and package conventions
- [ ] DTO/entity separation enforced
- [ ] Validation at boundaries; business rules in services
- [ ] State machine transitions match `spec/state-machine.md`
- [ ] Error responses match `.cursor/rules/api-standards.mdc`
- [ ] No hardcoded secrets or credentials
- [ ] Transaction boundaries correct
- [ ] Database access correct (indexes, migrations, no unsafe ddl-auto in prod)
- [ ] Logging appropriate; no sensitive data logged
- [ ] Tests exist for new behavior (unit + integration where required)
- [ ] State-machine integration tests updated if transitions changed
- [ ] Edge cases handled (not found, invalid input, invalid transitions)
- [ ] No unnecessary abstractions or dead code
- [ ] No deprecated APIs without justification

## Severity Classification

Report findings with exactly one severity:

| Severity | Meaning |
|----------|---------|
| **CRITICAL** | Security flaw, data loss risk, broken state machine, spec violation that breaks acceptance criteria |
| **HIGH** | Incorrect behavior, missing validation, wrong HTTP semantics, untested critical path |
| **MEDIUM** | Maintainability issue, inconsistent patterns, incomplete error handling |
| **LOW** | Style, naming, minor improvement |

## Output Format

For each finding:

1. **Severity**
2. **Location** (file and line or symbol)
3. **Issue** (what is wrong)
4. **Evidence** (spec rule or expected behavior)
5. **Recommendation** (concrete fix)

If no issues: state clearly that review passed, listing what was checked.

## Rules

- Do not invent issues to fill a report.
- Distinguish spec gaps (fix the spec first) from code bugs.
- Flag deviations from approved architecture as HIGH or CRITICAL.
