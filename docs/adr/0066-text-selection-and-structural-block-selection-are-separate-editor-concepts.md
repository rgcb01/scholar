# 0066 - Text Selection And Structural Block Selection Are Separate Editor Concepts

Status: Accepted

## Context

Scholar now edits both text blocks and non-text scientific object blocks. Text positions cannot accurately represent whole-object selection without fake offsets.

## Decision

Editor state uses an EditorSelection with TextSelection and BlockSelection variants. Exactly one selection mode is active at a time.

## Alternatives Considered

- Encode object selection as a fake DocumentPosition. This would corrupt text-only position semantics.
- Add parallel optional block-selection fields. This would permit invalid mixed states.

## Consequences

Text editing logic remains text-based, while structural object editing can grow without destabilizing DocumentPosition or DocumentRange.
