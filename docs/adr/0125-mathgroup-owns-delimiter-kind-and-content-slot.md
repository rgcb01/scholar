# 0125 - MathGroup Owns Delimiter Kind And Content Slot

## Status

Accepted

## Context

Groups need to preserve whether they were authored as parentheses, brackets, or braces without storing delimiter glyphs as editable child atoms.

## Decision

`MathGroup` stores semantic content plus one `MathDelimiter` value. Its editable child is addressed through `GroupContent`, and delimiter glyphs are derived from `MathDelimiter` at presentation and plain-text fallback boundaries.

## Alternatives Considered

- Store left and right delimiter symbols as editable AST children.
- Use separate node classes for each delimiter pair.
- Represent groups as sequences with special opening and closing atoms.

## Consequences

Group content stays structurally clear and easy to address. Delimiter rendering is centralized. Asymmetric delimiter pairs are not supported by the initial model.
