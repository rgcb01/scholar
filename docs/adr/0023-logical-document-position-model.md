# ADR 0023: Logical Document Position Model

## Status

Accepted

## Context

Scholar needs to place and move a caret through immutable paragraph content that may contain multiple marked `Text` nodes. The AST intentionally has no persistent node IDs and adjacent text nodes are not automatically normalized.

## Decision

Represent the first editor position as `blockIndex + characterOffset`, where `characterOffset` is relative to a paragraph's logical editable text stream.

The offset is not a raw UTF-16 index and is not tied to an inline node identity.

## Alternatives Considered

- Structural positions with `blockIndex + inlineIndex + textOffset`.
- Persistent AST node IDs.
- Minecraft visual coordinates as positions.

## Consequences

Positions survive inline splitting and replacement better than inline-index paths. Editing transformations must map logical offsets back to `Text` nodes, and unsupported inline nodes must fail explicitly until richer policies exist.
