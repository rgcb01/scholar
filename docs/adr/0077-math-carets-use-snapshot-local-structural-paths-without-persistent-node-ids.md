# 0077 - Math Carets Use Snapshot-Local Structural Paths Without Persistent Node IDs

Status: Accepted

## Context

The document model intentionally does not use persistent node IDs. Math editing still needs stable-enough addresses inside the current immutable expression snapshot for navigation, hit testing, and caret geometry.

## Decision

Math carets use `MathPath`, a snapshot-local structural path composed of path segments such as sequence child indices and fraction slots. These paths are not persistent IDs.

## Alternatives Considered

- Add persistent IDs to math AST nodes. This would conflict with the current document identity strategy.
- Use global linear offsets. This would make nested math structure harder to represent accurately.

## Consequences

Caret paths are simple value data and remain independent of persistence. Mutations must rewrite the expression and return an updated selection valid for the new snapshot.
