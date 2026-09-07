# ADR 0186 - Diagram Structural Edits Preserve Reference Validity In One History Transaction

## Status

Accepted

## Context

M17E adds node creation/deletion and connection creation/deletion to a semantic graph whose `DiagramConnection` endpoints must always resolve to existing element ports. A naive node deletion could temporarily leave dangling endpoints, and a separate diagram-specific history would diverge from Scholar's established editing model.

## Decision

All diagram structural edits remain immutable `DiagramBlock` replacements committed through the existing global `EditorHistory`.

Deleting a node removes every incident connection in the same semantic edit before constructing the replacement `DiagramDefinition`. Adding a generic node creates a valid rectangular node with four midpoint perimeter ports (`LEFT`, `RIGHT`, `TOP`, `BOTTOM`) so it is immediately connectable without introducing domain-specific semantics.

Connection deletion removes only the selected semantic connection. Selection is deterministically repaired to another valid connection, element, or the diagram title after structural deletion.

## Alternatives Considered

- Allow a temporarily invalid diagram and repair dangling connections in a later pass.
- Delete a node first and incident connections as separate undo steps.
- Give newly added generic nodes no ports until a later port-authoring tool exists.
- Introduce a diagram-local undo stack.

## Consequences

Every committed `DiagramDefinition` remains valid, Undo/Redo treats each human structural command as one action, and M18/M19 can reuse the same mechanics without inheriting electrical or mechanical meaning in the generic M17 model.
