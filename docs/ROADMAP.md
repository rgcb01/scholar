# Roadmap

This roadmap is milestone-based and deliberately revisable. It does not plan the full physics simulator or any separate STEM laboratory project.

## Current Implementation Status

The repository has advanced substantially beyond the original early-milestone outline below. The historical milestone descriptions are retained as design history, while current implementation status is summarized here:

- M0-M4 foundations are complete: repository/document core, Markdown interchange, read-only rendering, and scientific math foundation.
- The document Writing Engine v1 is complete through the M14 hardening work, including structured prose editing, selection/history/clipboard, visual-line navigation, equations, and structured math authoring.
- M15 Tables is complete: semantic tables, layout/rendering, cell editing, row/column operations, native clipboard + TSV fallback, and restricted Markdown table interchange.
- M16 Scientific Plots is complete through M16G hardening and final full-Gradle/manual regression validation: semantic static XY plots, auto-range/ticks/transforms, LINE/SCATTER rendering, plot editing, native clipboard, and responsive/extreme-numeric hardening.
- M17 Scientific Diagram Foundation is complete through M17G with full manual regression acceptance: semantic canvas/nodes/ports/connections, deterministic routing, hit testing, embedded editing/dragging, structural editing, native clipboard, and responsive/degenerate-geometry hardening.
- M18A-M18G are complete/manual QA accepted: electrical architecture, static symbols, orientation/hit testing, full authoring, explicit junctions + derived nets, workspace scaling, clipboard/interchange regression, and final hardening.
- M18 is complete/manual QA accepted through M18H.1 final terminal-length polish.
- M19A through M19E.1 are complete/manual QA accepted: primitives, dimensions/callouts, scrollable menus, constraints, symbols, and annotation visual polish. M19F Mechanical Assembly & Part References is implemented and pending manual QA.


## Current Forward Milestone Plan

### M17 - Scientific Diagram Foundation — Complete

- M17A Architecture & Data Model Design.
- M17B Static DiagramBlock + responsive layout/render + atomic selection.
- M17C Ports, connections, deterministic orthogonal routing, and pure-Java hit testing.
- M17D Dedicated diagram editing mode and one-transaction element dragging.
- M17E Explicit structural node/connection editing and label editing.
- M17F Lossless whole-diagram clipboard plus readable plain-text fallback.
- M17G Hardening, regressions, responsive/edge-case QA, and documentation closeout.

M17 deliberately contains no electrical or mechanical domain vocabulary. The completed foundation is now consumed by M18/M19.

### M18 - Electrical Diagrams

- M18A Architecture & Symbol Model Design - accepted/documented.
- M18B Electrical component core, stable terminal schemas, first static symbols, and visual polish: resistor, capacitor, DC voltage source, and ground — complete/manual QA accepted.
- M18C Quarter-turn orientation, terminal/connection integration, mixed-element layout, electrical hit testing, remaining approved basic symbols, and visual polish — complete/manual QA accepted.
- M18D Electrical authoring: insert-by-kind, designator/value editing, rotate, drag, wire creation, deletion, and global undo/redo — complete/manual QA accepted.
- M18E Explicit junction authoring, derived semantic electrical nets, branch connectivity, named-net annotations, and no geometry-based net inference — complete/manual QA accepted.
- M18E.5 Diagram workspace scaling: larger/resizable logical canvas, viewport zoom/pan/Fit, global symbol scale, and variable DiagramBlock height — complete/manual QA accepted.
- M18F Whole-diagram clipboard/plain-text regression for electrical elements and mixed diagrams — complete/manual QA accepted.
- M18G Responsive/edge-case hardening, regressions, documentation, and final manual QA — complete/manual QA accepted.
- M18H/H.1 Final electrical visual polish: compact adaptive electrical terminal exits plus shorter rendered component legs while preserving logical anchors, snapping, hit testing, connection endpoints, and net semantics — complete/manual QA accepted.

M18 models schematic diagrams only. Physical Minecraft components on breadboards/perforated boards remain a separate future STEM Lab representation.

### M19 - Mechanical Diagrams

- M19A Mechanical primitives: line, centerline, rectangle, circle, arc, arrow, and reference point — complete/manual QA accepted.
- M19B Dimensions and callouts — complete/manual QA accepted.
- M19B.1 Generic scrollable dropdown menus for long authoring menus — complete/manual QA accepted.
- M19C Geometric constraints/relationships: horizontal, vertical, coincident, parallel, perpendicular, and concentric — complete/manual QA accepted.
- M19D Mechanical symbols: shaft, gear, bearing, spring, piston, and bolt — complete/manual QA accepted.
- M19E/E.1 Mechanical labels, notes, leader callouts, and annotation visual polish — complete/manual QA accepted.
- M19F Mechanical Assembly & Part References: semantic item balloons linked by stable element IDs plus generated BOM tables — implemented/manual QA pending.
- M19G Hardening — planned.
- M19H Visual polish — planned.

M19 remains schematic/technical-drawing authoring rather than physical simulation. M19C uses a bounded deterministic relationship reconciler, not a general CAD solver.

## Milestone 0 - Repository Foundation

Goal: Establish a professional, maintainable NeoForge project foundation.

Non-goals: No Scholar document features, gameplay systems, UI, Markdown, math, networking, or speculative APIs.

Main deliverables:

- Clean NeoForge 1.21.1 project.
- Minimal mod metadata and entry point.
- Initial repository documentation.
- Basic repository hygiene.
- Initial testing strategy.

Exit criteria:

- `.\gradlew.bat build` succeeds.
- Documentation captures current scope without freezing unresolved decisions.
- The repository remains single-purpose and free of premature feature code.

## Milestone 1 - Document Core

Goal: Create a minimal Minecraft-independent document model.

Non-goals: No Markdown parser, no Minecraft rendering, no WYSIWYG editor, no math rendering.

Main deliverables:

- Initial immutable document model types.
- Basic validation rules for model invariants.
- Unit tests for core document behavior.

Exit criteria:

- Core document logic is testable without launching Minecraft.
- The model can represent a small structured document with text and simple hierarchy.
- Accepted ADRs capture the initial document structure, mutability, identity, text formatting, and extensibility decisions.

## Milestone 2 - Minimal Markdown Interchange

Goal: Support a small Markdown subset for import and export.

Non-goals: No full CommonMark implementation, no HTML passthrough, no math extension, no editor.

Main deliverables:

- Explicit supported Markdown subset.
- Parser from supported Markdown into the document model.
- Serializer from the document model back to supported Markdown.
- Round-trip and edge-case unit tests.
- Recoverable diagnostics for recognizable unsupported or malformed syntax.

Exit criteria:

- Supported Markdown behavior is documented and tested.
- Unsupported syntax is handled predictably.
- `Document -> serialize -> parse -> equivalent Document` round trips are tested.

## Milestone 3 - Read-Only Document Rendering

Goal: Render a minimal document viewer inside Minecraft.

Non-goals: No editing, no custom authoring tools, no complex layout engine, no networking.

Main deliverables:

- Read-only viewer for the minimal document model.
- Basic text layout and scrolling.
- Separation between model logic and Minecraft rendering code.
- Minecraft-independent layout model and text measurement abstraction.
- Temporary development client command for opening the viewer.
- Hardcoded development document parsed through Scholar Markdown.
- Layout unit tests.

Exit criteria:

- A simple document can be viewed in-game.
- Rendering code does not become the document source of truth.
- Layout behavior is covered by ordinary unit tests where Minecraft is not required.
- Manual visual testing confirms readable wrapping, formatting, clipping, and scroll behavior.

## Milestone 4 - Scientific Math Foundation

Goal: Add a small explicit mathematical notation foundation.

Non-goals: No full TeX engine, no CAS, no automatic solving, no broad math-layout completeness effort.

Main deliverables:

- Approved math notation architecture.
- Minecraft-independent Math AST.
- Structural tests for the Math AST.
- Accepted ADRs for math model, syntax separation, layout separation, and integration order.
- Minecraft-independent math layout for the first display equation slice.
- `EquationBlock` display equation integration.
- Minecraft rendering of laid-out math glyphs and fraction rules.

Exit criteria:

- Basic scientific notation can be represented structurally and rendered as a display equation.
- Unsupported notation boundaries are documented.
- Math core remains independent from Minecraft, NeoForge, Markdown, rendering, and document integration.

## Milestone 5 - Editing Vertical Slice

The first document typography slice is complete. Scholar v0.1 uses provisional document fonts while further typography tuning is deferred.

Goal: Build a basic WYSIWYG editing slice against the document model.

Non-goals: No full word processor, no collaborative editing, no advanced plugin API, no broad Markdown editing mode.

Main deliverables:

- Basic in-game editing workflow.
- Simple text and structure edits applied to the document model.
- Save/load path for edited documents.

Exit criteria:

- A user can create or modify a small document without editing raw Markdown.
- Editing operations preserve model invariants.
