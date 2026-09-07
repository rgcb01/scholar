# ADR 0006 - Scholar Markdown Parsing Strategy

## Status

Accepted

## Context

Scholar uses a dedicated semantic document model as its source of truth. Markdown is an interchange format, not the internal representation. The project intentionally supports carefully chosen subsets of large technologies rather than completeness projects.

## Decision

Scholar will use a purpose-built Markdown parser and serializer for its explicitly supported Markdown subset. It will not adopt a third-party full Markdown or CommonMark parser for the initial Markdown interchange layer.

This ADR accepts the high-level strategy only. It does not accept the detailed Scholar Markdown v0.1 grammar, diagnostics API, or parser class design.

## Alternatives Considered

- Use a full third-party Markdown parser and map its AST into Scholar's document model.
- Store Markdown directly as Scholar's source of truth.
- Defer Markdown interchange entirely.

## Consequences

Scholar can keep Markdown behavior aligned with the capabilities of its document model and avoid pretending unsupported Markdown has complete semantics. The project takes on responsibility for clear specification, parsing tests, serialization tests, and compatibility decisions for each supported subset.
