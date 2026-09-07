# M19E.1 — Mechanical Annotation Visual Polish

M19E.1 is a rendering/layout-only follow-up to M19E.

## Changes
- Part-label visual footprints expand to text width plus technical padding.
- Note footprints expand to text width plus a small interaction margin.
- Leader callouts reserve extra width for both the leader geometry and the text.
- Leader text is positioned above the horizontal rule with an explicit visible gap.
- The leader arrow and elbow remain derived geometry.
- Edited annotation text does not rewrite authored semantic bounds.

## Preserved semantics
No changes were made to:
- annotation IDs
- annotation text persistence/editing
- authored logical bounds
- selection/history semantics
- undo/redo
- clipboard/plain-text semantics
- zoom/pan
- other mechanical/electrical elements

## Validation
- Minecraft-independent core compiles under Java 21.
- Full client package compiles under Java 21 against the existing narrow Minecraft/NeoForge stubs.
- 5 focused annotation tests pass, including 3 new M19E.1 layout regression tests.
