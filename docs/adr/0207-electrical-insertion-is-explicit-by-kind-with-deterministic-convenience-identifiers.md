# ADR 0207: Electrical Insertion Is Explicit By Kind With Deterministic Convenience Identifiers

## Status

Accepted

## Context

M18D needs in-editor component creation for the bounded first electrical vocabulary. Insertion must not infer a component kind from text or geometry, and it should produce usable reference designators without introducing a project-wide component database or simulator identity model.

## Decision

Electrical authoring exposes one explicit insertion action per approved `ElectricalComponentKind`. Each inserted component receives a deterministic diagram-local element ID, a deterministic convenience reference designator derived from the component catalog, `DEG_0` orientation, an empty authored value, and kind-specific canonical logical bounds placed deterministically inside the canvas.

The convenience reference is ordinary authored annotation and may be changed immediately by the user. It is not a global identity or simulation key.

## Consequences

- Insertion behavior is predictable, testable, and undoable as one global history edit.
- Diode and LED share the conventional `D` reference namespace while retaining distinct semantic component kinds/element IDs.
- Scholar does not infer electrical components from pasted text, nearby wires, or symbol-like geometry.
- Automatic schematic layout and component databases remain deferred.
