# ADR 137 - First Table Layout Uses Equal Available Width Columns

## Status

Accepted

## Context

M15B needs static table rendering that works in the existing document layout pipeline without solving column sizing heuristics.

## Decision

The first `TableBlock` layout uses equal column widths across the available document content width, distributing any remainder pixels deterministically to earlier columns.

## Alternatives Considered

- Size columns to content.
- Allow user-defined column widths.
- Add a table measurement negotiation pass.

## Consequences

Layout is deterministic, simple to test, and responsive to viewport width. Content-aware sizing, resizable columns, and persisted column widths remain deferred.
