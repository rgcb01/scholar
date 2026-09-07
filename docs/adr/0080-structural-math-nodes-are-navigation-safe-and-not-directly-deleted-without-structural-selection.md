# 0080 - Structural Math Nodes Are Navigation Safe And Not Directly Deleted Without Structural Selection

Status: Accepted

## Context

Milestone 12B introduces caret movement through fractions but does not introduce math range selection or structural deletion commands.

## Decision

Caret navigation may enter and exit structural math nodes such as fractions, but simple Backspace/Delete only remove simple atoms. Structural nodes are not directly deleted without future structural selection semantics.

## Alternatives Considered

- Delete an entire fraction with a single Backspace/Delete at its edge. This is surprising without object or range selection.
- Skip structural nodes during navigation. This would make existing fraction content inaccessible.

## Consequences

Existing fractions remain editable and navigable in their slots, while destructive structural editing is deferred until Scholar has explicit semantics for selecting structural math objects.
