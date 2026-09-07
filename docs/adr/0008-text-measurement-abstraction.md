# ADR 0008 - Text Measurement Abstraction

## Status

Accepted

## Context

Minecraft's font renderer knows actual glyph widths, but using it directly inside layout would make layout Minecraft-dependent and difficult to test.

## Decision

The layout engine uses a small `TextMeasurer` abstraction for text width and line height. Minecraft provides a client implementation backed by the default font, and tests can use deterministic fake measurers.

## Alternatives Considered

- Let layout depend directly on Minecraft `Font`.
- Use fixed-width measurements everywhere.
- Build a custom font engine.

## Consequences

The layout package remains Minecraft-independent and testable. The abstraction is intentionally small, so advanced typography may require extending it later.
