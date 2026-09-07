# ADR 0160 - Plot Auto Range Does Not Force Zero

## Status

Accepted

## Context

Scientific XY plots often need to emphasize variation within the observed data range. Automatically forcing zero into every axis can hide meaningful variation and is not appropriate for the general XY plot model.

## Decision

For linear XY plots without an explicit axis range, Scholar derives the range from all authored static series values on that axis and adds deterministic five-percent padding. Zero is not forced into the range. Empty plots use `[0, 1]`. Constant zero data uses `[-1, 1]`; other constant values receive a small symmetric expansion.

## Alternatives Considered

- Always include zero.
- Use renderer-specific autoscaling.
- Require every plot to author explicit ranges.

## Consequences

Scientific plots can fit their data naturally while explicit ranges remain available whenever an authored scale is required.
