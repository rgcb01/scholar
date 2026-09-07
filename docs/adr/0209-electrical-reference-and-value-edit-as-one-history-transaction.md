# ADR 0209: Electrical Reference And Value Edit As One History Transaction

## Status

Accepted

## Context

An electrical component has two primary authored annotations in the first M18 vocabulary: reference designator and value. Editing them through unrelated single-field operations would create unnecessary history fragmentation and a more cumbersome in-game workflow.

## Decision

Enter on a selected `ElectricalComponent` opens one two-field component editor for reference designator and value. Applying the popup replaces both authored strings together in one immutable edit and therefore one `EditorHistory` transaction. Applying unchanged values is a no-op.

These fields remain plain presentation strings and are not parsed as executable quantities.

## Consequences

- One Undo restores both annotations to their previous values.
- The UI remains compact and specific to the semantic component while selection/history remain generic diagram infrastructure.
- Future typed quantity editing can be introduced behind a separate quantity model without changing the current AST strings implicitly.
