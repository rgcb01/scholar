# ADR 0272 - One Input Event Has One Authoritative Consumer

## Status

Accepted

## Context

Complex editor states make double consumption dangerous: a key press might activate a popup and edit text, or a mouse click might close a context menu and also change document selection.

## Decision

Each input event is consumed by one authoritative layer. Higher-priority layers may consume events for isolation even when they do not produce a semantic mutation.

## Alternatives Considered

- Allow bubbling after partial handling.
- Use mutation-diff detection to decide whether lower layers may continue.

## Consequences

Input behavior is deterministic and easier to test. Some no-op interactions intentionally consume input to protect the active mode.

