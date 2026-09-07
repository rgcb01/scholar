# ADR 151 - Markdown Tables Use Restricted Headered Pipe Subset

## Status

Accepted

## Context

Scholar now has a semantic `TableBlock` AST and needs Markdown interchange without taking on full GitHub table behavior, spreadsheet semantics, CSV, alignment, or multiline cells.

## Decision

Scholar Markdown supports only pipe tables with outer pipes, one header row, one separator row, at least one column, rectangular rows, and cell content limited to the currently supported inline Markdown subset.

## Alternatives Considered

- Support optional outer pipes.
- Support headerless tables.
- Support a broader GitHub table grammar immediately.

## Consequences

The parser remains deterministic and easy to reason about. Some common Markdown table variants remain unsupported until Scholar has corresponding semantic table features.
