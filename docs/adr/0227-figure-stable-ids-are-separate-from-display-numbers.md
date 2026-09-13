# ADR 0227: Figure stable IDs are separate from display numbers

## Status
Accepted for M20.

## Context
Future cross-references need a durable semantic target, while visible numbering changes with document order.

## Decision
Store a required stable string ID on each `FigureBlock`; derive the displayed number separately during layout, rendering, and plain-text clipboard serialization.

## Alternatives Considered
- Use displayed figure numbers as reference targets.
- Omit figure identity until cross-references are implemented.

## Consequences
Future references can survive renumbering. Clipboard paste must avoid creating duplicate stable IDs in the destination document.
