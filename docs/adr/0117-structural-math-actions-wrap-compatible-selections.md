# 0117 - Structural Math Actions Wrap Compatible Selections

## Status

Accepted

## Context

Fraction insertion can wrap a compatible selection as structural content. Square root insertion needs consistent behavior without broadening unsupported cross-slot mutation semantics.

## Decision

Structural math actions may wrap selections that are already safely extractable by the editor. Square root insertion wraps a compatible selection as the radicand. Unsupported ranges remain disabled or no-op according to existing action conventions.

## Alternatives Considered

- Only allow structural insertion at collapsed carets.
- Flatten unsupported selections into textual content.
- Generalize cross-slot selection mutation immediately.

## Consequences

Users can build structure around existing notation while the editor keeps conservative mutation rules. Future structures can reuse this pattern where their primary content slot is clear.
