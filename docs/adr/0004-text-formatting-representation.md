# ADR 0004 - Text Formatting Representation

## Status

Accepted

## Context

Scholar needs to represent inline text with semantic formatting such as bold and italic. Formatting should not be confused with renderer-specific presentation such as pixels, colors, font sizes, or Minecraft text components.

## Decision

Inline text is represented as `Text` plus a set of semantic `TextMark` values. The initial mark set contains only `BOLD` and `ITALIC`. Adjacent text nodes are not automatically normalized or merged during construction.

## Alternatives Considered

- Text nodes with boolean formatting fields.
- Nested formatting nodes such as `Bold -> Text`.
- A richer style object or CSS-like system.
- Automatic normalization of adjacent text with identical marks.

## Consequences

Marks support overlapping bold and italic text without deep nested formatting nodes. The model remains semantic and avoids presentation-specific styling. Deferring automatic normalization protects future editor cursor positions, selections, document history, and transformation semantics until normalization can be designed explicitly.
