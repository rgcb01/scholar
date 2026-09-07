# 0069 - Non-Text Blocks Are Navigated As Structural Objects

Status: Accepted

## Context

EquationBlock is not text-editable but must be reachable by mouse and keyboard.

## Decision

Plain Left and Right navigation moves between text carets and structural object selections. Shift navigation remains text-only and does not create mixed ranges.

## Alternatives Considered

- Keep object blocks as hard barriers for all navigation. This would make structural objects difficult to select without the mouse.
- Let Shift navigation cross objects. Mixed text/object selection is deferred.

## Consequences

Object blocks can be selected and deleted predictably while preserving existing text selection barrier behavior.
