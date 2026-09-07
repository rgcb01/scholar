# 0122 - Removing Final Empty Script Slot Unwraps To Base

## Status

Accepted

## Context

`MathScript` requires at least one script slot, but authoring allows users to explicitly remove empty slots.

## Decision

Backspace at the start of an empty script slot removes that slot. If no subscript or superscript remains, the script unwraps to its base.

## Alternatives Considered

- Leave an invalid script with no slots.
- Make empty-slot Backspace a no-op.
- Delete the entire base and script structure.

## Consequences

The AST invariant remains intact and deletion is explicit. Undo/redo must restore the previous script structure and selection.
