# 0110 - Math Inter-Atom Spacing Is Derived From Semantic Atom Relationships

## Status

Accepted

## Context

Semantic math tokens such as `MathNamedOperator` and `MathText` need to compose with neighboring math atoms without storing authored whitespace in the AST. For example, `MathText("if")` must not visually collide with adjacent numbers or identifiers, while `MathNamedOperator("sin")` should remain compact before an opening delimiter.

## Decision

Math inter-atom spacing is determined in Minecraft-independent layout code from relationships between neighboring sequence children. The AST remains unchanged: no fake whitespace nodes, no token-content mutation, and no spacing metadata in clipboard payloads or plain-text serialization.

The spacing model is a small Scholar-specific policy rather than a full TeX atom/glue system.

## Alternatives Considered

- Store spaces in `MathText` or surrounding token content.
- Insert explicit whitespace AST nodes during layout.
- Put spacing decisions in the Minecraft renderer.
- Implement a full TeX math atom spacing model immediately.

## Consequences

Semantic math text and named operators render legibly while source structure stays exact. Layout remains testable without Minecraft. More complete math spacing can be introduced later by evolving the centralized policy.
