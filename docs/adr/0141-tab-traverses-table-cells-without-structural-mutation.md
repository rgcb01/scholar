# ADR 141 - Tab Traverses Table Cells Without Structural Mutation

## Status

Accepted

## Context

Users expect Tab to move through table cells, but row and column mutation is out of scope for the first editing slice.

## Decision

Tab moves to the next cell in row-major order and Shift+Tab moves to the previous cell. Traversal clamps at the final and first cells and never creates rows.

## Alternatives Considered

- Create a new row from Tab in the final cell.
- Exit the table at the ends.
- Move with arrow keys between cells.

## Consequences

Cell traversal is predictable and mutation-free. Future row creation can be introduced as an explicit table-structure milestone.
