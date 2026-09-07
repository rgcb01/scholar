# ADR 135 - Table Headers Are Semantic Rather Than Inline Formatting

## Status

Accepted

## Context

Scientific tables commonly need a header row, but header presentation should not mutate user-authored inline marks.

## Decision

`TableBlock` stores `headerRowCount` as semantic table metadata. In the first slice this value may be `0` or `1`; renderer/layout may derive bold presentation for header rows without changing cell `InlineContent`.

## Alternatives Considered

- Encode header cells by adding `TextMark.BOLD`.
- Use a separate `TableHeaderRow` class.
- Defer header semantics entirely.

## Consequences

Header styling stays distinct from authored text formatting. Future import/export and accessibility behavior can inspect table semantics directly.
