# ADR 0263 - Heading Split Preserves Only The Original Left ID

## Status

Accepted

## Context

Headings may carry stable IDs for section navigation and cross-references. Splitting a heading in the middle creates two headings, and duplicating the original ID would violate document validation.

## Decision

When splitting a non-empty heading in the middle, the left heading keeps the original stable ID and the newly created right heading has no stable ID. Heading start/end Enter behavior keeps the original heading ID on the original heading.

## Alternatives Considered

- Duplicate the ID onto both headings. This creates validator errors.
- Generate a new ID for the right heading. This may be appropriate later, but current editor operations do not have an approved section-ID generation policy for arbitrary split text.

## Consequences

The original section identity remains deterministic and no duplicate heading IDs are introduced. Users or future tooling can assign an ID to the new section explicitly.
