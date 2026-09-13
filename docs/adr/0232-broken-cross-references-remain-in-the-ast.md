# ADR 0232: Broken Cross References Remain In The AST

## Status

Accepted

## Context

Deleting a target can leave existing references without a valid destination. Automatically deleting or rewriting references could lose author intent.

## Decision

Keep unresolved `CrossReference` nodes unchanged. Render and plain-text operations show `[Missing reference]` deterministically.

## Alternatives Considered

- Cascade-delete references when targets are removed. This is destructive and surprising.
- Convert broken references to plain text. This loses the target identity needed if the target returns through undo or later repair.
- Hide broken references. This makes document problems harder to find.

## Consequences

Broken references are visible, deterministic, and undo-friendly. Future validation tooling can locate unresolved references without changing the AST.
