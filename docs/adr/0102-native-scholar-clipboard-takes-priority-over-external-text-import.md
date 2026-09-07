# 0102 - Native Scholar Clipboard Takes Priority Over External Text Import

## Status

Accepted

## Context

M12F introduced process-local structured clipboard sidecar data so Scholar-to-Scholar paste can preserve exact AST structure even though the OS clipboard contains only text.

## Decision

If the current OS clipboard text matches a valid Scholar sidecar snapshot, Paste uses the native structured payload and does not run external text import. If the sidecar is absent or stale, Paste may fall back to external import.

## Alternatives Considered

- Always parse OS text first.
- Prefer external text when it is importable.
- Embed hidden provenance markers in clipboard text.

## Consequences

Native structure round-trips exactly. External text paste becomes available without weakening the sidecar model.
