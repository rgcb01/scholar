# M17F Implementation Report — Diagram Clipboard / Interchange

## Scope

M17F adds lossless whole-`DiagramBlock` clipboard transfer plus a deterministic readable plain-text fallback. It deliberately does not add partial node/port/connection clipboard, external text inference, Markdown diagram syntax, JSON interchange, SVG/image transfer, Mermaid, or Graphviz import/export.

## Implementation

### Native Scholar payload

Added `dev.rgcb.scholar.diagram.clipboard.DiagramClipboardPayload`.

The payload stores the immutable `DiagramBlock` itself, preserving the exact semantic diagram AST, including:

- logical canvas dimensions;
- diagram title;
- authored element order;
- diagram-scoped element IDs;
- node logical bounds and labels;
- authored port order, IDs, labels, side, and normalized offset;
- connection source/target endpoint references and labels.

No layout, routing geometry, hit-test state, drag state, selection, viewport, or Minecraft state is stored in the payload.

### Plain-text OS fallback

Added `DiagramPlainTextSerializer`.

The fallback is deterministic and human-readable. It emits:

- diagram title;
- logical canvas size;
- each node with local ID and authored bounds;
- each port with local ID, side, normalized offset, and optional label;
- each connection as `element/port -> element/port` plus optional label.

Structural CR/LF/tab characters inside semantic strings are normalized to spaces only in this lossy text representation. The native payload remains exact.

The fallback is intentionally **not** a parser contract.

### Shared clipboard actions

`EditorSession` now extends the existing Table/Plot clipboard pipeline to diagrams:

- whole selected `DiagramBlock` copy writes the plain-text fallback and installs `DiagramClipboardPayload` in `ScholarClipboardService`;
- whole selected diagram cut writes the clipboard first, then removes the block as one history edit;
- native paste at an ordinary editable text position uses existing structural block insertion/replacement rules;
- native paste over a selected `DiagramBlock` replaces it in place and leaves `BlockSelection` on the replacement;
- pasting a native diagram over another atomic block type is rejected;
- whole-diagram copy/cut/paste remains disabled while inside `DiagramEditingSelection`;
- external diagram-looking text remains ordinary text when no matching native sidecar exists;
- changing the OS clipboard invalidates the native diagram payload through the existing exact-text sidecar rule.

## Tests

Added `DiagramClipboardTest` with 15 tests covering:

- deterministic readable fallback;
- Unicode and structural-whitespace sanitization;
- empty diagrams;
- exact native AST/reference-ID preservation;
- copy success and failed OS clipboard write;
- cut + Undo/Redo and failed cut safety;
- native structural paste at a caret;
- native paste over a text selection;
- selected DiagramBlock replacement + Undo/Redo;
- rejection over other atomic block selections;
- DiagramEditingSelection guard;
- stale sidecar invalidation;
- no external text inference.

The repository had an expected M17E baseline of 788 Gradle test executions. M17F adds 15 ordinary tests, so the expected full Gradle result is **803 tests**.

## Validation Performed In This Environment

- Full pure-Java Scholar core compiled successfully with Java 21 (`javac --release 21`), excluding the Minecraft/NeoForge client entry points that require external game dependencies.
- The 15 new diagram clipboard tests compiled and executed through a lightweight local JUnit-compatible harness.
- Existing Plot and Table clipboard suites were compiled and executed through the same harness as targeted clipboard regressions.
- Targeted result: **44 passed / 0 failed**.
- The new `diagram.clipboard` package has no Minecraft, NeoForge, or Mojang imports.
- A real `./gradlew test --no-daemon` was attempted, but Gradle wrapper download failed with `UnknownHostException: services.gradle.org`, so full Gradle validation remains authoritative on the user's machine.

## Documentation / ADRs

Updated:

- `docs/DIAGRAM_SPEC.md`
- `docs/PROJECT_SPEC.md`
- `docs/ROADMAP.md`

Added:

- ADR 0188 — native diagram clipboard preserves `DiagramBlock` AST;
- ADR 0189 — plain clipboard fallback is a readable structural summary;
- ADR 0190 — native diagram paste replaces selected `DiagramBlock`;
- ADR 0191 — external diagram text is not silently inferred as `DiagramBlock`.

## Manual QA Before Closing M17F

1. Select a whole diagram with `BlockSelection` (leave internal diagram editing with Escape first).
2. `Ctrl+C` and paste into Notepad: verify readable title/canvas/nodes/ports/connections.
3. Paste back into Scholar at a paragraph caret: verify structural insertion and exact diagram recovery.
4. Paste over another selected DiagramBlock: verify in-place replacement.
5. Undo and Redo the replacement.
6. `Ctrl+X` a selected diagram, then Undo/Redo.
7. While inside `DiagramEditingSelection`, verify whole-diagram Copy/Cut/Paste is not used.
8. Copy the plain fallback to an external editor, alter it, copy it back, and paste into normal text: verify it remains plain text rather than becoming a DiagramBlock.

M17F should remain open until full Gradle and manual QA pass.
