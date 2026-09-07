# ADR 131 - Non-Text Scientific Blocks Participate In Document Navigation As Atomic BlockSelections

## Status

Accepted

## Context

Scholar documents include non-text scientific blocks such as `EquationBlock`, and future documents are expected to include tables, plots, and diagrams.

## Decision

Plain document-level Up and Down navigation treats non-text blocks as atomic `BlockSelection` stops. It does not automatically enter embedded editing modes.

## Alternatives Considered

- Stop before every non-text block.
- Skip non-text blocks.
- Enter embedded editors automatically.

## Consequences

Document navigation can move through mixed scientific documents without surprising mode changes. Shift+Up and Shift+Down remain text selections and clamp at non-text barriers rather than creating mixed text/object selections.
