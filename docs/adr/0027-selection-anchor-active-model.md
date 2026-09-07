# 0027 Selection Anchor/Active Model

Status: Accepted

## Context

Scholar needs a first editable selection model that can represent forward and backward selections without storing editor state in the document AST.

## Decision

Represent selection in `EditorState` with an `anchor` endpoint and an `active` endpoint. The active endpoint is also the caret position. When both endpoints are equal, there is no selection.

## Alternatives Considered

- Store only a normalized range: simpler for replacement, but loses selection direction.
- Store selection on document nodes: rejected because selection is transient editor state.
- Keep a separate drag anchor in the Minecraft screen: rejected because it would duplicate editor state.

## Consequences

The document model remains immutable semantic data. Directional selection is available for Shift+Arrow and mouse drag behavior, while replacement still uses a normalized `DocumentRange`.
