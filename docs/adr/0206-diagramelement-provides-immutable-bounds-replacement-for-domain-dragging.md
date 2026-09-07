# ADR 0206: DiagramElement Provides Immutable Bounds Replacement For Domain Dragging

## Status

Accepted

## Context

M17D implemented logical drag for generic `DiagramNode`, while M18D needs the exact same preview/commit/cancel and one-history-transaction behavior for electrical components. Duplicating drag logic by domain would couple the editor to every future `DiagramElement` subtype and would undermine the purpose of the M17 foundation.

## Decision

`DiagramElement` exposes immutable `withBounds(DiagramBounds)` replacement. `DiagramEditor.moveElement(...)` operates on the selected generic `DiagramElement`, clamps the requested bounds to the logical canvas, and replaces the element through this contract.

`DiagramNode` and `ElectricalComponent` both implement the contract while preserving every semantic field except bounds.

## Consequences

- M18 electrical components reuse M17 drag preview, cancel, commit, and global history behavior without an electrical drag stack.
- Future M19 mechanical diagram elements can opt into the same movement model without changing `DiagramEditor`.
- Domain-specific resize semantics remain out of scope; `withBounds` is an immutable movement/replacement primitive, not a public resize UI promise.
- Core dragging remains pure Java and independent of Minecraft rendering.
