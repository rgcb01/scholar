# 0028 Reverse Layout Hit-Testing Boundary

Status: Accepted

## Context

Mouse placement needs to map rendered document coordinates back to logical document positions. The document and layout layers must remain independent of Minecraft.

## Decision

Introduce a Minecraft-independent `DocumentHitTester` that consumes `LaidOutDocument`, document-local coordinates, and `TextMeasurer`. It uses layout source ranges and measured text prefixes to choose the nearest valid text boundary.

## Alternatives Considered

- Estimate offsets with average glyph widths: rejected because proportional fonts and Unicode boundaries would be inaccurate.
- Put hit-testing in the Minecraft screen: rejected because it would mix editor semantics with rendering integration.
- Store extra hit boxes in the AST: rejected because hit boxes are layout artifacts.

## Consequences

Mouse hit-testing can be unit tested without launching Minecraft. The Minecraft screen only converts screen coordinates to document-local coordinates and applies the resulting position.
