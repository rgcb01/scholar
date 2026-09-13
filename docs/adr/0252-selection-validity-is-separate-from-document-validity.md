# ADR 0252 - Selection Validity Is Separate From Document Validity

## Status

Accepted

## Context

M24A validates persisted document structure and degraded document state. Editor selections are transient session state that must also remain valid, but they depend on the current editing mode and active document snapshot.

## Decision

Selection validity is centralized in `EditorSelectionValidator` and checked by `EditorState`. The validator is pure Java, non-mutating, and separate from `DocumentValidator`.

## Alternatives Considered

- Put selection checks only in individual editor commands. That allowed invalid externally constructed states to survive until a later operation failed.
- Fold selection validation into document validation. That would mix persistent document diagnostics with ephemeral UI/session state.

## Consequences

Invalid selections fail at the editor-state boundary, and command tests can assert that navigation and mutations always return valid selections. Document validation remains about document content, not caret state.
