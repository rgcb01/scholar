# ADR 0245: Dataset Plot Resolution Skips Missing Or Nonnumeric Rows

## Status

Accepted

## Context

Dataset-backed plots may point at columns containing missing or text values. Converting those values to zero would create misleading scientific output.

## Decision

When resolving a dataset-backed plot series, a row contributes a point only when both X and Y cells are numeric. Missing or nonnumeric rows are skipped.

## Alternatives Considered

- Convert missing/nonnumeric values to zero. This silently fabricates measurements.
- Fail the whole series on the first bad row. This makes mixed real-world datasets brittle.
- Render gaps explicitly. That requires additional line-segment semantics beyond M23.

## Consequences

Plots remain scientifically safer by avoiding invented points. Future milestones may add explicit gap rendering or validation warnings.
