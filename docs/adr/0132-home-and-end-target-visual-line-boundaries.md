# ADR 132 - Home And End Target Visual Line Boundaries

## Status

Accepted

## Context

Wrapped paragraphs and headings have visual line boundaries that differ from block boundaries.

## Decision

Home moves to the start of the current visual line, and End moves to the end of the current visual line. Shift variants extend the active endpoint to those same visual-line boundaries.

## Alternatives Considered

- Home and End always target block start and block end.
- Add Ctrl+Home and Ctrl+End in the same slice.

## Consequences

Home and End match ordinary wrapped-text editor expectations. Whole-document Ctrl navigation remains deferred.
