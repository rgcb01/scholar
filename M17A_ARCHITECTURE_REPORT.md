# M17A - Scientific Diagram Foundation Architecture & Data Model Design

## Result

M17A is complete as a design/documentation milestone. No production Java diagram implementation was added.

Baseline remains the validated M16G codebase (704 tests passed by the user before this design slice).

## Files Added

- `docs/DIAGRAM_SPEC.md`
- `docs/adr/0178-diagramblock-stores-semantic-diagramdefinition.md`
- `docs/adr/0179-diagram-connectivity-uses-diagram-scoped-reference-ids.md`
- `docs/adr/0180-diagram-geometry-uses-local-logical-canvas-coordinates.md`
- `docs/adr/0181-first-diagram-element-is-generic-rectangular-node-with-perimeter-ports.md`
- `docs/adr/0182-diagram-connections-reference-ports-while-routes-are-derived.md`
- `docs/adr/0183-diagram-layout-and-hit-testing-are-pure-java.md`
- `docs/adr/0184-diagram-editing-uses-dedicated-mode-and-completed-drags-are-single-history-transactions.md`
- `docs/adr/0185-electrical-and-mechanical-domain-semantics-are-deferred-above-m17-foundation.md`

## Files Updated

- `docs/adr/README.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

## Decisions Closed

1. `DiagramBlock` will own immutable semantic `DiagramDefinition` data; no rendered paths/pixels in AST.
2. Cross-reference identity is introduced only inside diagrams where connectivity proves a real need: element IDs scoped to one diagram and port IDs scoped to one element.
3. Diagram geometry uses an authored logical canvas, top-left origin, X right/Y down, and uniform responsive scaling into document width.
4. The first element is a generic rectangular node, not an electrical/mechanical symbol and not a universal arbitrary-shape framework.
5. Initial ports live on node perimeter sides with normalized offsets.
6. Connections reference ports semantically; orthogonal routed geometry is derived during pure-Java layout.
7. Obstacle avoidance, waypoints, curves, direction/arrows, multi-select, resizing, snapping, and rich labels are deferred.
8. `DiagramLayoutEngine`, coordinate mapping, routing, and hit testing remain Minecraft-independent.
9. Ordinary document selection stays atomic; internal editing gets dedicated `DiagramEditingSelection`.
10. A completed element drag is one global undo/redo transaction; live mouse movement is transient editor state.
11. M18 owns electrical vocabulary; M19 owns mechanical vocabulary. M17 will not pre-model either discipline.
12. M17F will use native whole-block clipboard plus readable text fallback, with no external-text inference.

## Planned M17 Slices

- M17B: static model + layout/render + atomic selection.
- M17C: ports/connections/routing/hit testing.
- M17D: editing mode + dragging.
- M17E: structural editing.
- M17F: clipboard/interchange.
- M17G: hardening/final regression.

## Next Implementation Target

M17B should implement the approved semantic types and a development fixture with two labeled generic nodes connected through perimeter ports. The block should render responsively inside Scholar and participate in document insertion/navigation/deletion/undo as an atomic BlockSelection. M17B should not yet implement internal diagram editing or electrical/mechanical symbols.
