# 0072 - Structural Block Insertion May Replace A Text Selection Atomically

Status: Accepted

## Context

Scholar supports both text selections and structural object selections. Scientific blocks such as equations need to replace selected text without exposing intermediate delete-then-insert states.

## Decision

A non-collapsed `TextSelection` may be replaced by a `BlockNode` through one editor transformation. The transformation removes the selected text range, preserves surviving prefix/suffix content, inserts the structural block at the normalized selection start, selects it with `BlockSelection`, and records one history transaction.

## Alternatives Considered

- Delete the selection and then insert the block as two commands. This would expose an intermediate text state and create noisy history.
- Add mixed text/object selections first. This is broader than the current milestone requires.

## Consequences

Insert Equation can work over same-block, multi-block, and boundary-only text selections while preserving undo/redo as one user action.
