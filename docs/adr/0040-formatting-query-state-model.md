# 0040 Formatting Query State Model

Status: Accepted

## Context

Menus and toolbar controls need to represent whether Bold or Italic is active for the current selection or caret.

## Decision

Use `FormattingState` for editor-level mark queries: `OFF`, `ON`, and `MIXED`. Expose UI-facing action state through `ActionSelectionState`: `NOT_APPLICABLE`, `OFF`, `ON`, and `MIXED`.

## Alternatives Considered

- A boolean selected flag on actions.
- Formatting-specific UI logic in toolbar/menu widgets.
- Storing mixed state in the document.

## Consequences

Core formatting state remains Minecraft-independent. Toolbar and menu widgets can display toggle state without inspecting the document AST.
