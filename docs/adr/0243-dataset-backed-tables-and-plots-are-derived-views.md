# ADR 0243: Dataset-Backed Tables And Plots Are Derived Views

## Status

Accepted

## Context

Scholar needs tables and plots that can present the same dataset while preserving one semantic source of truth.

## Decision

A table or plot series with a dataset binding is resolved during layout/export into a current visual snapshot. The binding remains the authored semantic state.

## Alternatives Considered

- Mutate table rows and plot points whenever the dataset changes. This copies derived state into the AST.
- Make datasets a special kind of table only. This would force plots to depend on table layout semantics.
- Route dataset views through Markdown. Markdown is not Scholar's source of truth.

## Consequences

Dataset changes propagate without mutating view blocks. Layout and plain-text/Markdown export must resolve dataset-backed views before rendering or serializing.
