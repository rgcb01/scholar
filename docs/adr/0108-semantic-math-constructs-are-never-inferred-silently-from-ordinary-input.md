# 0108 - Semantic Math Constructs Are Never Inferred Silently From Ordinary Input

## Status

Accepted

## Context

Plain keyboard input and external clipboard text are ambiguous. For example, sin may be ordinary adjacent letters, a named operator, or part of a word. Scholar is not a CAS and should not guess mathematical intent without an explicit authoring action.

## Decision

Ordinary keyboard input and external plain-text import do not silently create `MathNamedOperator`, `MathText`, structural fractions, scripts, or other semantic constructs. Such constructs require explicit structured authoring or structured native clipboard data.

## Alternatives Considered

- Recognize common function keywords automatically.
- Interpret pasted words as named variables or text.
- Convert slash or other syntax into structural nodes by default.

## Consequences

Authoring remains predictable and reversible. Future autocomplete, command palette, or conversion UI can offer semantic construction only after explicit user acceptance.
