# ADR 0268 - Redo Restores Snapshots With Stable IDs

## Status

Accepted

## Context

Pasting and wrapping can generate stable IDs for headings, equations, tables, figures, and datasets. Redo must not rerun generators and produce different IDs.

## Decision

Redo restores the previously stored `EditorState` snapshot. It does not re-execute the original command.

## Alternatives Considered

- Re-run edit commands during redo.
- Store command parameters plus an ID seed.

## Consequences

Redo is deterministic and restores exact semantic documents. Snapshot history uses more memory than a command journal, but that tradeoff remains appropriate for the current editor foundation.
