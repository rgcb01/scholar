# Pre-M32 Computation Dependency Transfer

This is a narrow extension of the M25 extraction, planning, and materialization pipeline. It does not define M32 variables, expressions, evaluation, or persistence.

## Identity and source contract

`StableIdentityKind.VARIABLE` is a document-global identity namespace. A variable's authored name is presentation and lookup information, never transfer identity. A future document block that defines a variable or contains bound computation operands implements the document-layer `ComputationTransferBlock`. It exposes an optional defined variable ID, variable dependencies in stable occurrence order, and an immutable ID-rewrite operation. Each dependency is a document-layer `VariableDependencyReference` with an authoritative source variable ID. The fragment identity index derives provided and referenced variable keys from those semantic blocks; it does not invent identities or resolve names. The document model does not depend on the transfer package.

## Planning decisions

Planning handles each dependency occurrence before any destination mutation:

1. If the variable definition travels in the fragment, use the fragment's namespace-aware identity remap.
2. If it stays outside, preserve its ID only when M25's runtime document token and exact target witness prove that the same semantic target survives in the destination.
3. Otherwise reject the entire structured transfer with `UNRESOLVED_EXTERNAL_VARIABLE_DEPENDENCY`.

The third outcome is a planning failure, not a successful plan disposition. There is no text degradation, name-based recovery, raw-ID fallback, sentinel ID, or partial insertion. A same-name or same-ID variable in an unproven destination cannot capture the operand. Identity reservations are operation-local; a rejected plan commits nothing and creates no history transaction.

`VariableDependencyPlan` is separate from the existing `ReferenceDispositionPlan`. The accepted CrossReference policy, including its text degradation, is unchanged. Materialization only consumes validated decisions; it does not resolve names, allocate IDs, or recheck provenance. Future editor insertion must still stage and validate the complete resulting document as one M25 transaction. Undo/redo restores committed snapshots rather than replanning.

## M32 integration

M32 variable definitions and computed blocks must expose their stable IDs and bound expression references through `ComputationTransferBlock`. A future expression parser may resolve authored names during authoring, but the stored operand transferred by this contract is bound by ID. The block's immutable rewrite must update all matching expression operands while preserving authored expression structure and presentation. M32 must not add an independent clipboard dependency rule.

The interface is currently exercised by test-only block values. M32 will supply real semantic blocks, validator rules, persistence V2 encoding, and editor integration. No M32 model object or placeholder is persisted by this foundation.
