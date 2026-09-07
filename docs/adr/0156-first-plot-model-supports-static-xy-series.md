# ADR 0156 - First Plot Model Supports Static XY Series

## Status

Accepted

## Context

Scholar needs useful scientific plotting without becoming a general charting package before the basic plot pipeline is proven.

## Decision

The first plot model supports static XY data with authored-order finite `double` points and `LINE` or `SCATTER` series. Negative values, duplicate X values, unsorted values, empty series, and one-point series are valid. Bar charts, histograms, function plots, log axes, and dynamic sources are deferred.

## Alternatives Considered

- Implement many chart families immediately.
- Use parallel X/Y vectors.
- Use `BigDecimal` as the initial numeric representation.

## Consequences

The first model is small and directly useful for STEM data while preserving a clear path to later plot types. Data validation is deterministic and point order remains authored order.
