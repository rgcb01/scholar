# 0030 First-Slice Cross-Paragraph Selection Policy

Status: Accepted

## Context

Milestone 5D introduces mouse selection, but cross-paragraph editing, paragraph merging, and multi-block range transformations are not part of this slice.

## Decision

Keep `DocumentRange` limited to one block. During mouse drag, the editor screen hit-tests within the anchor paragraph and clamps movement to that paragraph's start or end when the pointer leaves it.

## Alternatives Considered

- Implement cross-paragraph selection immediately: rejected as too broad for the first selection slice.
- Allow invalid cross-block ranges temporarily: rejected because downstream editing operations would need speculative behavior.
- Disable drag outside the paragraph: rejected because clamping gives predictable edge behavior.

## Consequences

The first mouse selection slice remains small and safe. Multi-block selection and paragraph-level editing can be designed later without changing the document AST.
