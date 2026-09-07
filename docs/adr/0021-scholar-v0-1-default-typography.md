# ADR 0021: Scholar v0.1 Default Typography

## Status

Accepted for v0.1 as a provisional project default.

## Context

Scholar needs a default document typography configuration before continuing into editing work. The typography architecture already separates document content from Minecraft shell UI and resolves fonts downstream from semantic document and math nodes.

The completed readability and glyph-coverage comparison selected a practical v0.1 default without claiming permanent superiority or educational outcomes.

## Decision

Use Source Sans 3 for document BODY and HEADING roles.

Use Noto Sans Math for the MATH role and as the controlled scientific fallback for document text when Source Sans 3 does not cover a glyph.

Keep Minecraft shell/UI text on the default Minecraft font.

This decision does not prohibit future typography changes.

## Alternatives Considered

- Keep Minecraft default typography for the document surface.
- Use Atkinson Hyperlegible Next for document BODY and HEADING roles.
- Use Noto Sans for document BODY and HEADING roles.
- Keep multiple A/B profiles in the development viewer.

## Consequences

Scholar v0.1 has one clean default typography path instead of experiment-specific command modes. Font resources and licenses are limited to the fonts still distributed with the mod.

Future typography tuning, zoom, themes, or user font preferences remain separate future work.
