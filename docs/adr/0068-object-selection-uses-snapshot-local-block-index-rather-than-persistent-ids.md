# 0068 - Object Selection Uses Snapshot-Local Block Index Rather Than Persistent IDs

Status: Accepted

## Context

The immutable document model intentionally has no persistent node IDs.

## Decision

BlockSelection stores a snapshot-local block index. Edits construct a new explicit resulting selection.

## Alternatives Considered

- Add persistent IDs to document nodes. This is unnecessary for snapshot-based history and would add identity concerns to value data.
- Re-find inserted blocks by equality. Duplicate object values make this unsafe.

## Consequences

History snapshots restore the matching document and selection together, while insert operations report the exact inserted index directly.
