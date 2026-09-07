# M19E — Mechanical Labels & Annotations

Implemented semantic mechanical annotations:

- Part Label — boxed editable identifier.
- Note — editable technical note.
- Leader Callout — editable text with derived leader/arrow geometry.

Integration includes diagram menu actions, selection/hit testing, drag through the existing generic element path, delete, undo/redo, existing text popup editing, layout/rendering, native clipboard compatibility, plain-text fallback, and a `Mechanical Annotations` development fixture.

Validation in this environment:

- Minecraft-independent Java 21 core compilation: PASS.
- DevelopmentDocument fixture compilation against core: PASS.
- Gradle wrapper attempted, but the sandbox cannot resolve `services.gradle.org`, so authoritative Gradle tests must be run locally.

Manual QA: open `Mechanical Annotations`, then add one annotation manually, double-click/edit its text, move it, delete it, and undo.
