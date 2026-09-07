# Scholar Typography Specification

## Status

Accepted v0.1 provisional default.

## Purpose

Scholar typography separates the Minecraft UI shell from the Scholar document surface. The game shell continues to use normal Minecraft typography, while document content uses the current Scholar v0.1 provisional defaults.

These defaults are accepted for v0.1 and may evolve later.

## Typography Roles

Scholar document presentation uses semantic typography roles:

- `BODY`
- `HEADING_1`
- `HEADING_2`
- `HEADING_3`
- `HEADING_4`
- `HEADING_5`
- `HEADING_6`
- `MATH`

These roles do not contain Minecraft font objects, resource locations, or semantic document content.

## Default Profile

`ScholarTypography.defaultProfile()` currently defines the established layout behavior:

- paragraph, heading, equation, and page width policy values;
- role-specific bold/italic defaults;
- role-specific text colors;
- line-height adjustment for heading level 1;
- math rule thickness.

The Minecraft client typography resolver maps this profile to the v0.1 document fonts.

## v0.1 Font Defaults

The current provisional defaults are:

Document body and headings:

- Primary: Source Sans 3
- Scientific fallback: Noto Sans Math
- Last resort: Minecraft default/unifont fallback providers

Document math:

- Primary: Noto Sans Math
- Last resort: Minecraft default/unifont fallback providers

Minecraft shell/UI:

- Minecraft default font

License files for bundled fonts are preserved under `docs/licenses/fonts`.

## Measurement And Rendering Consistency

The same `ScholarTypography` instance must drive document layout measurement, math layout measurement, and document-surface rendering.

Minecraft-specific font lookup is isolated under the client rendering boundary by `MinecraftTypographyResolver`.

## Shell vs Document Boundary

Scholar typography applies only to the document surface:

- headings;
- paragraphs;
- text marks;
- display equations.

Minecraft shell UI such as buttons, controls, tooltips, chat, and general screen chrome should continue to use normal Minecraft UI typography unless a future decision explicitly changes that boundary.

## Future Direction

Future work may tune spacing, introduce zoom or user preferences, or revisit the font defaults. Those are not implemented yet.
