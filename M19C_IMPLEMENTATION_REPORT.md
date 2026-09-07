# M19C Implementation Report — Mechanical Constraints / Relationships

## Scope

M19C introduces first-class semantic geometric relationships for mechanical diagrams:

- Horizontal
- Vertical
- Coincident
- Parallel
- Perpendicular
- Concentric

Constraints are not stored as decorative lines or labels. They are `MechanicalConstraint`
diagram elements that reference authored `MechanicalPrimitive` IDs. Their on-screen markers
are derived from the referenced geometry.

## Directional primitive orientation

M19C extends directional mechanical primitives (`LINE`, `CENTERLINE`, `ARROW`) with a minimal
quarter-turn `MechanicalOrientation`:

- `DEG_0`
- `DEG_90`

The original three-argument `MechanicalPrimitive` constructor remains source-compatible and
defaults to `DEG_0`.

Applying Horizontal/Vertical or Parallel/Perpendicular constraints changes semantic orientation
and swaps the authored footprint around its center when needed so line length is preserved.

## Relationship behavior

### Unary
- Horizontal: forces a directional primitive to `DEG_0`.
- Vertical: forces a directional primitive to `DEG_90`.

### Binary
- Parallel: the peer adopts the subject's orientation.
- Perpendicular: the peer adopts the subject's quarter-turn orientation.
- Coincident: M19C v1 aligns primitive centers. Endpoint-level coincidence is intentionally
  deferred because M19A does not yet expose editable endpoint handles.
- Concentric: restricted to circle/arc primitives and aligns their centers while preserving size.

M19C intentionally implements a small deterministic reconciliation pass rather than a general CAD
constraint solver. Constraints are reapplied after diagram semantic edits and during drag preview/
commit so their relationships remain active.

## Authoring UX

Unary constraints are applied directly to the selected mechanical primitive.

Binary relationships use transient two-step authoring:

1. Select the source primitive.
2. Choose `Start Coincident`, `Start Parallel`, `Start Perpendicular`, or `Start Concentric`.
3. Select the peer primitive.
4. Press Enter or choose `Finish Mechanical Constraint`.

Escape / `Cancel Mechanical Constraint` cancels only the transient source state and creates no
history entry.

The pending source is transient UI/editor state and is never serialized.

## Editing / integration

- Constraint markers participate in diagram hit testing and selection.
- Constraint marker dragging is intentionally disabled; marker placement is derived.
- Delete removes the selected constraint as one global history edit.
- Deleting a mechanical primitive also removes constraints that reference that primitive.
- Undo/redo clears pending transient constraint authoring.
- Whole-diagram native clipboard remains lossless.
- Plain-text fallback lists constraint kind plus subject/peer IDs and directional orientations.
- Zoom/pan, clipping, workspace resize, and generic diagram history infrastructure are reused.

## Visual language

Constraint markers are small programmatic technical glyphs:

- horizontal / vertical bar marker
- coincident cross + dot
- parallel slashes
- perpendicular T
- concentric double circle

No constraint symbol is persisted as authored pixel geometry.

## Validation performed in this environment

- Minecraft-independent main sources compile under Java 21.
- `DevelopmentDocument` compiles against the core.
- All client sources compile using narrow Minecraft/NeoForge API stubs, catching Java-level
  integration/signature errors in the changed screen and renderer.
- 10 M19C-specific tests pass in the local assertion harness.
- 16 existing diagram/electrical structural tests also pass in that harness after menu expansion.
- Total focused validation: 26/26 passing.
- Gradle wrapper execution was attempted but cannot download Gradle 9.2.1 because this sandbox
  cannot resolve `services.gradle.org`.

## Manual QA fixture

Open `Mechanical Constraints` in the development document and verify:

- a horizontal line with horizontal marker
- a vertical line with vertical marker
- two parallel lines with parallel marker
- a perpendicular line pair with perpendicular marker
- two concentric circles with concentric marker
- coincident reference points with coincident marker

For authoring, also test one binary relationship manually using the Start -> select peer -> Enter
workflow.
