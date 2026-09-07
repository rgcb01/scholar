# 0073 - Surviving Right Text After Structural Replacement Becomes Paragraph Body

Status: Accepted

## Context

Replacing a text selection with a scientific block may split the start and end text blocks. The surviving right suffix no longer remains in the original title context once a structural block separates it from the left side.

## Decision

When structural replacement creates a right suffix from selected endpoint text, that suffix is emitted as a `Paragraph`, regardless of whether the endpoint block was a `Paragraph` or `Heading`. The surviving left prefix keeps the start block style and heading level when non-empty.

## Alternatives Considered

- Preserve the endpoint block style on the right suffix. This can leave heading fragments after an inserted equation.
- Collapse both sides around the inserted block. This would remove the structural separation the command is creating.

## Consequences

Structural replacement produces document-friendly body continuation after equations while preserving left-owned style semantics from text replacement.
