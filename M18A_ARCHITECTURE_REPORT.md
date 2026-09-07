# M18A — Electrical Diagram Architecture & Symbol Model Design

## Result

M18A is complete as a design/documentation milestone. No production Java or tests were changed.

The starting point is the user-validated M17G codebase with **811 tests / 0 failures**. M17 Scientific Diagram Foundation is now considered closed and M18 consumes it directly.

## Files Added

- `docs/ELECTRICAL_DIAGRAM_SPEC.md`
- `docs/adr/0194-electrical-components-extend-diagramelement-inside-diagramblock.md`
- `docs/adr/0195-electrical-component-kinds-own-stable-terminal-schemas.md`
- `docs/adr/0196-electrical-symbols-use-derived-normalized-geometry.md`
- `docs/adr/0197-electrical-orientation-is-quarter-turn-and-preserves-terminal-identity.md`
- `docs/adr/0198-electrical-designators-and-values-are-authored-annotations-not-simulation-state.md`
- `docs/adr/0199-first-electrical-wiring-reuses-diagramconnection-without-net-inference.md`
- `docs/adr/0200-schematic-symbols-are-separate-from-physical-perfboard-representations.md`
- `docs/adr/0201-first-electrical-component-vocabulary-is-explicit-and-bounded.md`
- `docs/adr/0202-electrical-editing-reuses-diagrameditingselection-and-global-history.md`

## Files Updated

- `docs/adr/README.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

The ADR index also now includes the already-existing M17 ADRs 0186-0193, which were missing from the index even though the files themselves existed.

## Decisions Closed

1. M18 does **not** create an `ElectricalDiagramBlock`; electrical components extend `DiagramElement` inside the existing `DiagramBlock(DiagramDefinition)`.
2. The planned semantic element is `ElectricalComponent` with kind, logical bounds, quarter-turn orientation, reference designator, and value label.
3. Component kinds own stable terminal schemas. Existing `DiagramEndpoint(elementId, portId)` remains the connection identity contract.
4. Terminal placements are derived from component kind + orientation + bounds. Rotation preserves terminal IDs and therefore preserves existing wire endpoints.
5. Electrical symbols are derived pure-Java normalized geometry. The AST does not store zig-zags, circles, pixels, textures, or Minecraft draw calls.
6. Initial orientation is 0/90/180/270 degrees only; labels remain upright.
7. `referenceDesignator` and `valueLabel` are authored annotations, not executable quantities or electrical-solver state.
8. The approved first vocabulary is resistor, capacitor, DC voltage source, ground, diode, LED, and SPST switch. M18B proves the architecture with resistor/capacitor/source/ground first.
9. First-pass wires reuse M17 `DiagramConnection` and orthogonal routing. No net inference from crossings/touching geometry is allowed.
10. Free-space junctions, net IDs, buses, and richer net semantics are deferred to an evidence-driven M18E checkpoint rather than pre-designed now.
11. Electrical editing reuses `DiagramEditingSelection`, M17 drag/connection-draft behavior, and global `EditorHistory` rather than creating a parallel editor/history stack.
12. Whole-diagram native clipboard remains the lossless transfer boundary; electrical plain-text fallback will be descriptive only and never inferred back into AST.
13. Scholar electrical schematics are explicitly separate from future physical Minecraft breadboard/perfboard components and footprints in the STEM Lab.
14. Electrical simulation, SPICE/netlist execution, PCB layout, arbitrary-angle rotation/mirroring, and universal/user-defined symbol systems remain outside M18's first scope.

## Planned M18 Slices

- **M18B** — Electrical component core + stable terminal schemas + first static symbols (resistor, capacitor, DC source, ground).
- **M18C** — Quarter-turn orientation + terminal/connection integration + mixed-element layout/hit testing + remaining approved basic symbols.
- **M18D** — Electrical authoring: insert by kind, edit designator/value, rotate, drag, connect, delete, undo/redo.
- **M18E** — Electrical connectivity hardening and junction/net design checkpoint based on actual editor evidence.
- **M18F** — Whole-diagram clipboard/plain-text regression for electrical and mixed diagrams.
- **M18G** — Responsive/edge-case hardening, regression, docs, and final QA.

## M18B Implementation Target

M18B should make the first real schematic visible in `/scholar_dev_editor`, conceptually:

```text
              Basic DC Circuit

       + ┌───────┐
      V1 │   ○   │─────────── R1 10 kΩ ──────────┐
     5 V │       │                                │
       - └───────┘                                │
          │                                       │
          └─────────────── GND ───────────────────┘
```

The exact visual symbol shapes should follow the approved semantic/symbol pipeline rather than this ASCII approximation.

M18B should prove:

- `ElectricalComponent` as a second `DiagramElement` implementation;
- stable derived terminals;
- normalized pure-Java symbol geometry;
- responsive layout/render inside the existing diagram canvas;
- mixed generic/electrical handling without duplicating M17;
- ordinary whole-diagram atomic selection still works.

M18B should **not** yet add the complete electrical editing menu, net/junction semantics, simulation, physical perfboard models, or arbitrary symbol plugins.
