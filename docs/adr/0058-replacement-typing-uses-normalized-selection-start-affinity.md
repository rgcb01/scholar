# 0058 Replacement Typing Uses Normalized Selection Start Affinity

Status: Accepted

## Context

Multi-block selections preserve anchor/active direction, so the active endpoint may be visually or logically after the intended replacement insertion point.

## Decision

Typing replacement marks are derived from the normalized selection start, not from the active endpoint or a combination of selected content.

## Alternatives Considered

- Use active endpoint affinity.
- Preserve explicit typing marks from before selection.
- Compute a mixed formatting state across selected text.

## Consequences

Forward and backward selections with the same normalized range produce identical replacement text. Rich multi-block formatting remains a later concern.
