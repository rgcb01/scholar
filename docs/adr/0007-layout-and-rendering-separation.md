# ADR 0007 - Layout and Rendering Separation

## Status

Accepted

## Context

Scholar needs to render semantic documents in Minecraft without turning the document model into Minecraft UI state. Layout should be testable without launching the game, while rendering must still use Minecraft's GUI APIs.

## Decision

Scholar uses a hybrid rendering pipeline: document model to Minecraft-independent layout, then laid-out document to Minecraft-specific renderer and screen.

## Alternatives Considered

- Render the document model directly from a Minecraft screen.
- Build a large standalone UI/layout framework.
- Defer layout separation until rendering becomes more complex.

## Consequences

Layout behavior can be unit tested with fake measurements, and Minecraft-specific drawing remains isolated under client packages. The renderer must translate laid-out document coordinates into viewport coordinates, which adds a small boundary layer.
