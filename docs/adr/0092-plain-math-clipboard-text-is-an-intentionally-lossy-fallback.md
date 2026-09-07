# 0092 - Plain Math Clipboard Text Is An Intentionally Lossy Fallback

## Status

Accepted

## Context

The OS clipboard must contain useful text for external applications even when Scholar also has structured sidecar data.

## Decision

Scholar emits deterministic, human-readable plain math text as a fallback. It is not a canonical round-trip serialization format.

## Alternatives Considered

- Emit an encoded Scholar payload.
- Emit LaTeX as the default.
- Emit Markdown math.

## Consequences

Plain clipboard text is understandable in ordinary destinations, but structural information such as true stacked fractions may be lossy without the sidecar.
