# 0107 - Descriptive Text Inside Math Uses Explicit MathText

## Status

Accepted

## Context

Scientific notation sometimes includes upright descriptive text such as if, where, otherwise, or for all. This text is part of mathematical notation, but it is not a variable, named operator, document paragraph, or Markdown span.

## Decision

Scholar represents intentionally authored textual content inside math with `MathText(String content)`. Internal whitespace is preserved exactly. Leading and trailing whitespace are invalid so mathematical layout spacing remains separate from text content.

## Alternatives Considered

- Store math text as `MathIdentifier`.
- Reuse document `Text` nodes inside math.
- Add whitespace-sensitive layout behavior to ordinary token strings.

## Consequences

Math text can later receive upright typography and structured export behavior without polluting identifier semantics. Editing and authoring UX for creating math text remain future work.
