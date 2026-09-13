# ADR 0234: Markdown Serializes Cross References As Resolved Text

## Status

Accepted

## Context

Scholar Markdown does not yet define syntax for semantic cross-references. Inventing syntax now would prematurely turn M21 behavior into a public interchange format.

## Decision

For now, Markdown serialization emits the currently resolved readable label, or `[Missing reference]` for unresolved references. Markdown parsing does not reconstruct semantic `CrossReference` nodes.

## Alternatives Considered

- Add custom reference syntax immediately. This would broaden the Markdown contract.
- Encode hidden metadata in Markdown text. This would be opaque and brittle.
- Reject documents containing references. This would make export less useful.

## Consequences

Markdown export remains readable but lossy for references. Full semantic Markdown round-trip is explicitly deferred.
