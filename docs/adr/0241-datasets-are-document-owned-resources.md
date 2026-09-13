# ADR 0241: Datasets Are Document-Owned Resources

## Status

Accepted

## Context

Tables and plots need to share scientific data without duplicating values into every visual view. If each view owns its own data, edits cannot reliably propagate.

## Decision

`Document` owns immutable `ScientificDataset` resources separately from its ordered block list. Dataset-backed blocks store bindings to those resources rather than copying the dataset contents.

## Alternatives Considered

- Store dataset values directly inside every table and plot. This duplicates data and makes propagation impossible.
- Add a separate global dataset registry outside the document. This would make documents incomplete without ambient state.
- Defer datasets until persistence exists. That would block the table/plot shared-data slice.

## Consequences

One dataset edit can update every derived table and plot view. Document construction now validates duplicate dataset IDs, and future persistence must serialize dataset resources with the document.
