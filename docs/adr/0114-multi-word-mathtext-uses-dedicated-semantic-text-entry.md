# 0114 - Multi-Word MathText Uses Dedicated Semantic Text Entry

## Status

Accepted

## Context

`MathText` preserves internal whitespace, but ordinary math input deliberately does not create whitespace atoms. Scholar needs a way to author text such as `for all` without changing ordinary math typing semantics.

## Decision

Multi-word `MathText` is authored through a compact semantic token popup with an explicit content field. The popup can create or edit `MathText` while keeping whitespace inside the semantic node itself.

## Alternatives Considered

- Add ordinary whitespace atoms to math input. This would blur layout spacing and semantic text content.
- Limit first-slice `MathText` to single words. This would miss a primary use case.

## Consequences

Multi-word math prose is possible without introducing fake whitespace nodes. The popup remains application-shell UI and does not become a general equation editor.
