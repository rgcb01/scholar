# ADR 0246: Native Dataset Clipboard Remaps Duplicate Dataset IDs

## Status

Accepted

## Context

Copying a dataset into a document that already contains the same dataset ID would violate document invariants.

## Decision

Native dataset paste preserves the copied dataset structure but remaps the dataset ID when needed to avoid duplicates.

## Alternatives Considered

- Reject duplicate dataset paste. This makes repeated experiments awkward.
- Overwrite the existing dataset. This risks destroying shared data unexpectedly.
- Allow duplicate IDs. That makes view resolution ambiguous.

## Consequences

Dataset paste remains safe and predictable. Views copied separately still need dependency-aware remapping in a later milestone.
