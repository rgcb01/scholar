# ADR 0250 - Broken References And Bindings Are Valid Degraded Document State

## Status

Accepted

## Context

Users may delete a referenced heading, figure, table, equation, dataset, or dataset column while preserving surrounding authored content. Existing rendering already has deterministic missing-reference and missing-dataset behavior.

## Decision

Unresolved `CrossReference` targets and missing dataset or column bindings produce validation warnings, not errors. They are valid degraded document state.

## Alternatives Considered

- Cascade-delete references and bindings when targets disappear. This would destroy useful user intent.
- Make broken references invalid. This would make ordinary document edits hard to stage.

## Consequences

Scholar can keep visible placeholders and later offer repair workflows. Validation reports the degradation without changing the AST.
