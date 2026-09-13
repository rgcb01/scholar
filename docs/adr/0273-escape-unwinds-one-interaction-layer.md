# ADR 0273 - Escape Unwinds One Interaction Layer

## Status

Accepted

## Context

Escape can close popups, cancel diagram drags, cancel connection or constraint authoring, or exit nested editing. Handling several of these at once would make the editor feel unpredictable.

## Decision

Escape unwinds only the highest-priority active interaction layer.

## Alternatives Considered

- Make Escape globally exit every active transient mode at once.
- Reserve Escape only for closing the screen.

## Consequences

Users can back out predictably from nested interactions. Every future transient mode must define its Escape behavior relative to the dispatch priority.

