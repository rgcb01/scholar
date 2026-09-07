# ADR 0020 - Measurement and Rendering Typography Consistency

## Status

Accepted

## Context

Document layout and math layout depend on text measurements. If measurement uses one typography configuration while rendering uses another, wrapping, centering, clipping, and equation alignment can become incorrect.

## Decision

The same `ScholarTypography` profile must drive both measurement and rendering. Minecraft-specific font lookup is centralized in a client-side typography resolver instead of duplicated across measurers and renderers.

## Alternatives Considered

- Give each measurer and renderer its own font/style decisions.
- Hardcode current Minecraft font assumptions in layout.
- Delay typography consistency until adding a custom font.

## Consequences

Future font changes can be introduced more safely. The current renderer remains visually equivalent while gaining a single source of truth for document-surface typography.
