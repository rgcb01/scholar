# 0096 - Scholar Does Not Serialize Java Objects Into The Clipboard

## Status

Accepted

## Context

Clipboard data can come from untrusted external applications, and Java object serialization would create a dangerous and brittle interchange boundary.

## Decision

Scholar does not place Java-serialized objects on the clipboard and does not deserialize clipboard text or data into arbitrary Java objects.

## Alternatives Considered

- Use Java object serialization in a custom clipboard flavor.
- Encode class names or object graphs in visible text.

## Consequences

The v0.1 sidecar is trusted only because Scholar created it in memory. Future cross-process structured clipboard formats must use explicit bounded schemas rather than Java serialization.
