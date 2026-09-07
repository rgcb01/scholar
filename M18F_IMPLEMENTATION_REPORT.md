# M18F Implementation Report — Electrical Clipboard / Interchange Regression

## Post-Implementation Acceptance

Manual in-game QA was completed successfully by the user on 2026-09-06. M18F is closed/accepted; M18G is the active follow-up slice.

## Scope

M18F hardens and proves Scholar's existing whole-diagram clipboard boundary after M18E/M18E.5. It does not introduce partial component clipboard, external circuit parsing, Markdown diagram import, or another electrical-specific history stack.

## Native clipboard

`DiagramClipboardPayload(DiagramBlock)` remains the lossless Scholar-to-Scholar transfer representation. Explicit regression coverage now verifies preservation of:

- mixed generic `DiagramNode` + electrical elements;
- all component semantic fields already carried by the AST;
- component orientation and stable terminal ids;
- explicit `ElectricalJunction` and authored net label;
- all `DiagramConnection` endpoint ids and labels;
- derived net connectivity after paste;
- the M18E.5 authored `workspaceAspectRatio`;
- local diagram element ids when a whole diagram is duplicated into another block.

Local element ids are intentionally not regenerated for whole-block copy/paste. Their uniqueness contract is inside one `DiagramDefinition`, not across the entire document. Pasting the immutable block into another document position therefore preserves the diagram exactly without creating a global-id collision.

Whole-diagram cut/replacement paste remain one global undoable edit. Copy does not create history.

## Plain-text fallback

`DiagramPlainTextSerializer` remains deterministic and descriptive-only. Electrical component output now distinguishes electrical terminals from generic diagram ports and writes:

- stable terminal id;
- terminal semantic role;
- oriented perimeter side/offset.

Example:

```text
Component: RESISTOR R1 [r1]
Bounds: [45.0, 8.0, 14.0, 34.0]
Orientation: DEG_90
Value: 10 kΩ
Terminal: a [PASSIVE_A, TOP @ 0.5]
Terminal: b [PASSIVE_B, BOTTOM @ 0.5]
```

Existing junction bounds, four ports, optional `Net Label:`, and readable connection endpoints remain in the fallback. When M18E.5 authored workspace height differs from the canvas-derived default, the serializer adds `Workspace Aspect Ratio:`. The default ratio is omitted so legacy/default summaries stay compact.

## No external-text inference

The plain-text format is not an import format. Diagram-looking or electrical-looking external clipboard text is pasted as ordinary text. Native structure is restored only when the process-local Scholar sidecar payload still matches the current system clipboard text. If the clipboard text has changed, the stale sidecar is cleared and no diagram AST is inferred.

No netlist, SPICE, Markdown-diagram, or text-to-electrical parser is added.

## Tests

Added `ElectricalDiagramClipboardInterchangeTest` with 10 M18F regression tests covering:

1. readable electrical semantics, junction net label, connections, and authored workspace height;
2. exact native payload preservation and derived-net equivalence;
3. whole-diagram copy inside a mixed Scholar document;
4. native paste at a text caret with surrounding Heading/Paragraph/Equation content preserved;
5. diagram replacement paste + undo/redo;
6. atomic cut + undo;
7. intentional preservation of local element ids across separate diagram blocks;
8. no AST inference from external electrical-looking text;
9. stale sidecar invalidation after clipboard replacement;
10. legacy/default workspace fallback remaining compact.

Updated the existing electrical fallback regression to assert the new semantic `Terminal:` representation.

## Validation in this environment

- Java 21 pure-core production compilation: PASS.
- All non-Minecraft test sources compiled against a lightweight JUnit-compatible API stub: PASS.
- The 13 existing client/controller/UI tests were also compiled and executed using lightweight API-shape stubs for the two Minecraft-backed widget height constants they depend on: PASS.
- Reflection harness execution count: **907/907 passing**. This is the previous M18E.5 baseline of 897 executions plus 10 new M18F tests, including both values of the existing parameterized Markdown regression.
- A real `./gradlew test` was attempted. The Gradle wrapper cannot download Gradle 9.2.1 in this sandbox because `services.gradle.org` cannot be resolved (`UnknownHostException`), so no real Gradle/NeoForge success is claimed here.

Run the authoritative Gradle suite on the normal development machine before accepting M18F manually.

## Manual QA

1. Open `Junction + Net` and exit diagram editing so the whole `DiagramBlock` is selected.
2. `Ctrl+C`, move to a normal text paragraph, and `Ctrl+V`; verify an exact diagram block is inserted and selected.
3. Verify R/C/D/etc. designators, values, rotations, wires, the junction, `VOUT`, logical canvas size, and workspace height survived.
4. Undo/redo the paste.
5. Select the pasted diagram block, `Ctrl+X`, then undo; verify the exact circuit returns.
6. Copy the diagram, then overwrite the OS clipboard with ordinary text outside Scholar and paste in Scholar; verify ordinary text is inserted and no diagram is inferred.
7. Copy the diagram and paste into an external text editor; verify the summary is readable and electrical terminals appear as `Terminal: ... [ROLE, SIDE @ offset]`.

## Next

After M18F manual acceptance, proceed to M18G final responsive/edge-case hardening. M18H remains the final M18 electrical visual-polish pass for shortening rendered terminal/pin leads without changing logical anchors or connectivity.
