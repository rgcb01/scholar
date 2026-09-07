# ADR 129 - Vertical Text Navigation Uses Visual Layout Lines

## Status

Accepted

## Context

Scholar text blocks can wrap into multiple visual lines, and caret movement with Up and Down should follow what the user sees rather than raw character counts or block boundaries.

## Decision

Vertical text navigation uses the current `LaidOutDocument` as the source of truth for visual lines, run positions, and source ranges.

## Alternatives Considered

- Navigate by logical character offsets only.
- Recompute wrapping inside editor navigation.

## Consequences

Navigation matches rendered wrapping and formatted runs. The editor navigation layer now depends on layout snapshots and text measurement abstractions, but remains Minecraft-independent.
