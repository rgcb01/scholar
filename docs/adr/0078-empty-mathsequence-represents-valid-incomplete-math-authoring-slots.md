# 0078 - Empty MathSequence Represents Valid Incomplete Math Authoring Slots

Status: Accepted

## Context

Equation editing needs valid caret positions even when an equation or structural math slot has no visible content yet. Rejecting empty math sequences would make first input and later structural authoring awkward.

## Decision

`MathSequence` may be empty and represents a valid incomplete authoring slot.

## Alternatives Considered

- Require placeholder tokens in empty slots. This would put editor affordances into the semantic math AST.
- Reject empty sequences. This would prevent clean representation of an empty equation or empty future fraction slots.

## Consequences

Layout, hit testing, and caret geometry must handle empty math sequences. Serialization and rendering should treat emptiness as valid incomplete authoring state, not as an error.
