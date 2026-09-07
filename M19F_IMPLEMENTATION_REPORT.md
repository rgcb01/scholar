# M19F — Mechanical Assembly & Part References

## Scope
M19F connects authored mechanical drawing elements to structured assembly/document data without turning Scholar into a CAD package.

## Implemented
- `MechanicalPartReference` is a first-class semantic diagram element.
- Each part reference stores a stable target element ID, item number, part name, quantity, and description.
- The visible balloon, item number, and leader line are derived geometry.
- `Add Part Balloon` is enabled when a mechanical primitive or mechanical symbol is selected.
- Part names can be edited through the existing diagram text-editing path.
- `Generate BOM` creates a normal Scholar `TableBlock` immediately after the diagram with `ITEM | PART | QTY | DESCRIPTION`.
- BOM generation is one undoable history edit.
- Deleting a referenced primitive/symbol also removes dependent part references, preventing dangling references.
- Part balloons participate in hit-testing, movement, delete, undo/redo, native diagram clipboard, and plain-text serialization.
- A `Mechanical Assembly + BOM` development fixture was added with Shaft, Bearing, Gear, three balloons, and a matching table.

## Validation
- Minecraft-independent core compiles under Java 21.
- Full client package compiles under Java 21 against the existing narrow Minecraft/NeoForge stubs.
- Focused semantic tests cover stable-ID targeting and one-edit BOM generation/undo: 2/2 passing.
- Authoritative Gradle + visual/manual QA remains to be run on the user's machine.
