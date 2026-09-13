# ADR 0255 - Derived Inline Text Is Not Editable Source Text

## Status

Accepted

## Context

Cross-references and similar inline constructs render derived labels, while table-of-contents entries, figure numbers, and section numbers are also derived from document state. Editing the displayed label directly would confuse source identity with presentation.

## Decision

Atomic inline nodes such as `CrossReference` contribute one logical caret/selection unit, but their rendered labels are not editable source text. Derived display text remains presentation output unless a dedicated semantic editing command owns that source.

## Alternatives Considered

- Expand derived labels into editable text positions. That would make cursor movement depend on mutable display labels rather than AST source.
- Hide derived labels from navigation entirely. That would make selections and clipboard behavior skip visible content unexpectedly.

## Consequences

Caret movement stays source-based while still crossing visible inline references predictably. Future semantic editing for references can target the node itself without rewriting display labels as prose.
