# ADR 0198 - Electrical Designators And Values Are Authored Annotations Not Simulation State

## Status

Accepted

## Context

A schematic normally displays labels such as `R1`, `10 kΩ`, or `5 V`. Scholar is a scientific document engine, not yet an electrical simulator, and parsing display strings into executable quantities would prematurely couple notation to future simulation semantics.

## Decision

Electrical components initially store non-null authored `referenceDesignator` and `valueLabel` strings, either of which may be empty. Scholar may generate convenient default designators when inserting components, but uniqueness, prefix correctness, units, and numeric validity are not AST invariants.

Executable quantities and units, if later required, will use a dedicated typed model rather than silently parsing these presentation annotations.

## Alternatives Considered

- Parse every value label into SI units immediately.
- Store only one generic component label.
- Make designator uniqueness a document validity rule.

## Consequences

M18 can author readable educational schematics now without pretending to be a solver. Future simulation remains free to define a stronger quantity model.
