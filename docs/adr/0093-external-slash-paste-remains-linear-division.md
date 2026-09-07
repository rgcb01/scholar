# 0093 - External Slash Paste Remains Linear Division

## Status

Accepted

## Context

ADR 0084 keeps typed `/` as linear division rather than structural fraction creation.

## Decision

M12F does not parse external math text. Future external plain-text paste should initially interpret slash as linear division unless a later accepted design changes that behavior.

## Alternatives Considered

- Treat pasted `x/y` as a structural fraction.
- Make slash behavior depend on paste source.

## Consequences

Typing and future external paste semantics remain aligned. Structural fractions round-trip only through the Scholar-native sidecar in v0.1.
