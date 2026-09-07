# 0051 Multi-Block Logical DocumentRange

Status: Accepted

## Context

Scholar selection now needs to span adjacent editable text blocks while preserving block-local positions and anchor/active direction.

## Decision

`DocumentPosition` remains block-local. `DocumentRange` now supports normalized multi-block ranges ordered by block index and character offset. `EditorState` preserves raw anchor and active positions, including cross-block direction.

## Alternatives Considered

- Introduce global document text offsets.
- Add persistent node identifiers or path positions.
- Keep `DocumentRange` single-block and add a separate selection range type.

## Consequences

Selection semantics stay independent of layout and persistent identity. Document validity and editable-region checks remain responsibilities of editor and selection helpers, not `DocumentRange`.
