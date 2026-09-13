# ADR 0261 - Structural Edits Must Produce Valid Document And Selection

## Status

Accepted

## Context

Scholar now has many structural editing paths across text blocks, equations, tables, plots, diagrams, figures, TOC, datasets, clipboard, and context menus. These operations can invalidate either the document or the transient selection if each path chooses its own recovery behavior.

## Decision

Every structural edit must produce a document with no structural validation errors and a valid `EditorSelection`. Regression tests use `DocumentValidator` and `EditorSelectionValidator` as oracles after structural mutations.

## Alternatives Considered

- Trust individual operations without shared validation coverage. This leaves stale selection bugs hidden until manual QA.
- Run full validators after every production keystroke. That would be heavier than needed for M24D and may belong at explicit trust boundaries.

## Consequences

Structural bugs are easier to catch without moving validation into every runtime path. Future history hardening can rely on these postconditions.
