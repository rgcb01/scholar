# 0062 - Multi-Block Inline Formatting Uses ON OFF MIXED Semantics

Status: Accepted

## Context

Formatting buttons and shortcuts need consistent behavior across single-block and multi-block selections.

## Decision

Inline formatting state is ON when every selected character has the mark, OFF when none do, MIXED when some do, and NOT_APPLICABLE when no characters are selected. Toggle removes the mark for ON and adds it for OFF or MIXED.

## Alternatives Considered

- Disable mixed selections. This would prevent common rich-text editing workflows.
- Treat MIXED as ON. This would misrepresent the selected content and make toggling surprising.

## Consequences

A single Bold or Italic command applies one semantic mark transformation across all selected character ranges and preserves the original selection direction.
