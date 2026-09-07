# ADR 0201 - First Electrical Component Vocabulary Is Explicit And Bounded

## Status

Accepted

## Context

Electronics contains an enormous symbol vocabulary. Attempting to model every passive, semiconductor, IC, connector, instrument, and package at once would turn M18 into a completeness project and obscure whether the base component/symbol architecture works.

## Decision

The approved first M18 vocabulary is limited to resistor, capacitor, DC voltage source, ground, diode, LED, and SPST switch. M18B begins with the smaller proof set: resistor, capacitor, DC voltage source, and ground.

The component abstraction must support one or more terminals so later multi-terminal devices do not require replacing it, but transistors/op-amps/ICs are not implemented merely to prove extensibility.

## Alternatives Considered

- Implement a comprehensive IEC/ANSI component library in M18B.
- Use one generic symbol with a text type field.
- Design a public user-defined symbol/plugin format before built-in symbols are proven.

## Consequences

The milestone stays testable and educationally useful while leaving room for later evidence-driven expansion.
