# 0094 - Cross-Slot Partial Math Ranges Are Not Copy Or Cut Targets

## Status

Accepted

## Context

M12D can represent some cross-slot math selections, but mutation deliberately avoids ambiguous half-fraction semantics.

## Decision

Math Copy and Cut are available only for selections that can be extracted as a canonical `MathSequence` fragment and safely deleted. Unsupported cross-slot partial selections are not Copy or Cut targets.

## Alternatives Considered

- Flatten cross-slot fragments to plain text.
- Invent half-structure clipboard fragments.
- Copy only visible glyph text.

## Consequences

Clipboard behavior does not broaden structural editing semantics or introduce misleading fragments. Some highlighted math ranges remain non-copyable in v0.1.
