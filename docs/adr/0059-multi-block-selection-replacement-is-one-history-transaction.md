# 0059 Multi-Block Selection Replacement Is One History Transaction

Status: Accepted

## Context

Deleting or replacing a multi-block selection may internally split two blocks, remove intermediate blocks, and insert replacement text.

## Decision

Each user command records one snapshot transaction: Delete, Backspace selection deletion, Cut, Paste replacement, Enter replacement, and the first typed replacement. Contiguous typing after replacement may coalesce into that typing transaction.

## Alternatives Considered

- Record internal delete and insert phases separately.
- Disable typing coalescing after multi-block replacement.
- Store command-specific inverse operations.

## Consequences

Undo restores the original document and multi-block selection direction in one step. Snapshot history remains the editor's history model.
