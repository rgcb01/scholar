# ADR 0200 - Schematic Symbols Are Separate From Physical Perfboard Representations

## Status

Accepted

## Context

The future STEM Lab may represent tiny physical components placed on a perforated board inside Minecraft. A physical resistor has package dimensions, leads, hole spacing, and world placement; a Scholar schematic resistor has a semantic kind, terminals, symbol orientation, and diagram position.

Conflating these models would make both document authoring and physical simulation less accurate.

## Decision

M18 models schematic components only. Physical breadboard/perfboard footprints, 3D/pixel models, lead spacing, board holes, and world placement belong to a separate future STEM Lab representation.

A later integration may map a shared semantic component kind across the two systems, but neither representation is stored as the other's AST.

## Alternatives Considered

- Use physical Minecraft component models directly as Scholar schematic elements.
- Store schematic symbol geometry as a physical footprint.
- Build the perfboard editor inside M18.

## Consequences

Scholar remains a clean scientific-document system, while future physical components can be optimized for the very small Minecraft-scale visual language without constraining schematic notation.
