# M19B Implementation Report — Dimensions & Callouts

## Scope
M19B adds semantic mechanical measurements on top of the M19A primitive vocabulary.

### Added dimension kinds
- Horizontal
- Vertical
- Aligned
- Radius
- Diameter
- Angle

`MechanicalDimension` is a first-class `DiagramElement`. Its displayed measurement is derived from authored logical geometry rather than stored as presentation text. This keeps M19B compatible with the future M19C constraint layer.

### Derived rendering
The Minecraft renderer derives:
- extension lines
- dimension lines
- arrowheads
- radius/diameter callouts
- angular rays/arcs
- centered measurement labels

### Editing
All dimension kinds can be inserted from the Diagram menu, selected/hit-tested, dragged through the existing domain-neutral DiagramElement drag path, deleted as one history edit, copied losslessly with a whole DiagramBlock, and represented in the external plain-text fallback.

### Explicit non-goals
M19B does not add constraints, associative references to primitive endpoints, units/preferences, tolerances, GD&T, or mechanical component symbols. Those remain later M19 work.

## Validation
- Pure Java non-client main sources compile under Java 21.
- M19B focused tests compile against the core.
- Added `MechanicalDimensionTest` with 7 focused tests.
- Existing explicit Diagram-menu expectations were updated for the M19B commands.
- Manual Minecraft QA should use the `Mechanical Dimensions` development fixture.
