# ADR 0264 - Dataset Resources Are Not Deleted With Dataset Backed Views

## Status

Accepted

## Context

Dataset-backed tables and plots are document blocks that resolve views from document-owned datasets. Deleting a view block could be confused with deleting the source dataset.

## Decision

Deleting a dataset-backed table or plot removes only the view block. Dataset resources remain document-owned resources and are edited/deleted only through dataset resource operations.

## Alternatives Considered

- Cascade-delete unused datasets when views are deleted. This could destroy data unexpectedly.
- Prevent deletion of dataset-backed views. This would make document structure too rigid.

## Consequences

View structure and resource structure remain separate. Broken bindings remain valid degraded state when resources are removed separately.
