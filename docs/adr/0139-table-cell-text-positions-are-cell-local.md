# ADR 139 - Table Cell Text Positions Are Cell Local

## Status

Accepted

## Context

Cell content is inline text, but offsets inside a cell should not depend on global document text ranges or rendered layout.

## Decision

Table cell editing uses row and column coordinates plus logical user-character offsets inside that cell's `InlineContent`.

## Alternatives Considered

- Use global text offsets across the table.
- Store rendered glyph positions in selection state.
- Introduce persistent cell IDs.

## Consequences

Selections remain stable across relayout and preserve the no-persistent-ID principle. Row and column mutations will need future selection remapping rules.
