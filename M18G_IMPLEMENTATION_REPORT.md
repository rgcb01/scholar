# M18G Implementation Report — Final Responsive / Edge-Case Hardening

## Status

Implemented on top of the user-accepted M18F project. Manual in-game QA was completed successfully by the user on 2026-09-06, so M18G is closed/accepted. The 924-test local-equivalent baseline remains the starting point for M18H.

M18G deliberately adds no new electrical component kinds, simulation behavior, partial-element clipboard, or new authored diagram semantics. M18H remains the separate final visual-polish pass for terminal/pin lead lengths.

## Hardening Changes

### Transient viewport rebasing

M18E.5 correctly kept `DiagramViewport` outside the document AST, but `ScholarEditorScreen` stored cameras directly by snapshot-local block index. Structural document edits can shift those indices, creating a risk that one diagram's zoom/pan is lost or accidentally inherited by another diagram.

M18G introduces pure-Java `DiagramViewportStore` and routes `ScholarEditorScreen` viewport reads/writes/Fit through it. On committed relayouts the store reconciles successive immutable `Document` snapshots:

- exact unchanged `DiagramBlock` identity follows normal block-index shifts;
- same-slot immutable diagram replacements retain the existing camera;
- cameras for deleted diagrams are discarded instead of leaking to shifted neighbors;
- ambiguous lossless clipboard duplicates start at Fit rather than sharing transient camera state;
- drag-preview documents are never used as reconciliation snapshots.

The viewport remains transient: no AST, clipboard, Markdown/plain-text, or history fields were added.

### Deterministic derived net observation

`ElectricalNetResolver` already builds connectivity deterministically, but immutable `Set.copyOf` / `Map.copyOf` constructors did not preserve that iteration order. M18G preserves the resolver's semantic insertion order with unmodifiable `LinkedHashSet` / `LinkedHashMap` copies.

This is only a determinism/reproducibility guarantee for the derived connectivity view. Net meaning remains independent of iteration order and continues to come only from explicit connections plus explicit junction internals.

## Regression Coverage

M18G adds 17 ordinary JUnit regressions across two suites.

`DiagramViewportStoreTest` covers:

1. same-index semantic diagram replacement retaining transient viewport;
2. insertion before a diagram rebasing its viewport forward;
3. deletion before a diagram rebasing its viewport backward;
4. deleting a diagram without leaking its camera onto a shifted neighbor;
5. a neighboring diagram retaining its own viewport after an earlier diagram is deleted;
6. lossless whole-block duplication starting the new duplicate at Fit rather than sharing the source camera.

`ElectricalDiagramHardeningTest` covers:

1. every approved component kind and every quarter-turn orientation at a canvas edge across very narrow through wide responsive widths;
2. minimum/maximum viewport zoom with stable workspace geometry and off-workspace hit rejection;
3. degenerate electrical wire routing at a one-pixel responsive layout;
4. component rotation at every logical-canvas corner while preserving explicit endpoints and derived connectivity;
5. four consecutive quarter-turns at an edge preserving topology and returning orientation;
6. extreme global symbol scale factors remaining bounded without topology mutation;
7. logical-canvas shrink translating edge content inward while preserving connectivity;
8. empty diagrams under extreme document width/zoom/viewport-center inputs;
9. narrow mixed generic/electrical/junction reflow without authored-AST mutation;
10. deterministic semantic endpoint iteration in derived nets;
11. chained rotate -> global scale -> canvas resize -> annotation editing with exact four-step undo and redo restoration.

## Validation In This Environment

- Java 21 pure-core production compilation: **PASS** (252 non-Minecraft production sources).
- All 80 test source files compile against a lightweight JUnit-compatible API stub plus API-shape stubs for the two Minecraft-backed shell height constants used by pure client UI tests: **PASS**.
- Reflection harness execution count: **924/924 passing**. This is the user-accepted M18F baseline of 907 executions plus 17 M18G tests, including both values of the existing parameterized Markdown regression.
- The generic/electrical layout hardening was also stress-probed during implementation across hundreds of thousands of canvas/orientation/document-width/workspace/viewport combinations before the final targeted regression set was written.
- A real Gradle/NeoForge test run is still authoritative and must be executed on the normal development machine.

## Manual QA Checklist

1. Run `./gradlew test` (or `.\\gradlew test` on PowerShell) and expect 924 successful test invocations.
2. Open `Junction + Net`; exercise minimum/maximum useful zoom, middle-mouse pan, and `Ctrl+0` Fit while selecting/dragging near all four workspace edges.
3. Resize the game window and change Minecraft GUI scale; verify diagram clipping and hit targets remain aligned.
4. Rotate and drag components against each logical-canvas edge/corner; wires/junctions/net labels must remain semantically connected.
5. Make the logical canvas smaller/larger and scale symbols down/up; verify elements stay inside the canvas and undo/redo restores each semantic step.
6. In a document with at least two diagrams, zoom/pan them differently, then insert/delete/cut/paste blocks before and between them. Verify a diagram never inherits the other diagram's camera after indices shift.
7. Copy/paste a whole electrical diagram once more as an M18F regression and verify the new duplicate starts in a sensible Fit view while the source remains usable.
8. Check an empty diagram and a mixed generic-node + electrical + junction diagram at narrow width.

## Next

M18G manual acceptance is complete. Proceed to M18H only: final electrical visual polish that removes terminal/wire endpoint protrusions without changing logical anchors, stable terminal IDs, snapping, hit testing, connection endpoints, or net semantics.
