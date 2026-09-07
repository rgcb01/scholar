# 0127 - Empty Math Groups Use Empty MathSequence Content

## Status

Accepted

## Context

The editor needs incomplete group templates with stable caret targets, but Scholar avoids placeholder characters or fake AST nodes.

## Decision

An empty group is represented as `MathGroup(MathSequence(List.of()), delimiter)`. Backspace at the start of that empty `GroupContent` slot removes the template; Delete inside the empty slot is a no-op.

## Alternatives Considered

- Store placeholder nodes.
- Store null content until the user types.
- Automatically remove empty groups during layout or serialization.

## Consequences

The AST remains immutable and structurally valid while users author incomplete notation. Empty groups are explicit editor state and can be removed by a focused authoring command.
