# ADR 0236: Section Numbers Are Derived Display State

## Status

Accepted

## Context

Section numbers change when headings are inserted, deleted, reordered, or restyled. Storing numbers in heading text or metadata would make documents stale and harder to edit.

## Decision

Section numbers are derived by `DocumentStructureResolver` and never stored in `Heading`. Rendering and plain-text export may show the derived number, but Markdown heading serialization remains clean and unnumbered.

Skipped heading levels are represented with explicit zero placeholders, such as `1.0.1`, so irregular structure is deterministic and visible.

## Alternatives Considered

- Store section numbers in heading content. This makes renumbering destructive and user-visible in the semantic model.
- Collapse skipped levels silently. This hides document-structure problems.
- Invent hidden placeholder sections. This creates synthetic nodes not present in the document.

## Consequences

Display labels are always current. Skipped levels do not crash, and their output clearly shows the irregular hierarchy.
