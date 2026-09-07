# ADR 0215 — Diagram Workspace Height And Logical Canvas Size Are Authored Separately

## Status
Accepted for M18E.5.

## Decision
`DiagramBlock` stores an authored `workspaceAspectRatio` in addition to its semantic `DiagramDefinition`.

The logical `DiagramCanvas` continues to define the coordinate space in which nodes, electrical components, junctions, and connection endpoints are authored. `workspaceAspectRatio` controls only the embedded document viewport height. A legacy one-argument `DiagramBlock` derives the workspace aspect directly from the logical canvas so pre-M18E.5 documents retain their previous fit layout.

Logical canvas resize is an explicit immutable editor operation. Existing element sizes are preserved; elements are translated inward if a reduced canvas would otherwise place them outside. A requested canvas smaller than an existing element is rejected.

Workspace-height changes and canvas resize are semantic document edits and therefore participate in global undo/redo.

## Consequences
- A larger logical canvas no longer forces the document block to become proportionally huge.
- The user can make a diagram viewport taller or shorter without rewriting diagram coordinates.
- The user can also deliberately resize the logical authoring extent when more schematic space is needed.
- Fit/zoom/pan remains independent transient state per ADR 0214.
