# ADR 0014 - Math Metrics and Baseline Model

## Status

Accepted

## Context

Mathematical notation needs more than paragraph text width and line height. Fractions and future inline math require baseline-aware dimensions so equations can align naturally with surrounding glyphs and nested structures.

## Decision

Scholar Math layout uses a dedicated `MathTextMeasurer` that returns width, ascent, and descent. Every laid-out math box exposes width, ascent, and descent, with children positioned relative to a parent baseline.

## Alternatives Considered

- Reuse the existing paragraph `TextMeasurer`.
- Use top-left-only boxes without baselines.
- Let the Minecraft renderer infer text and fraction metrics directly.

## Consequences

Math layout remains Minecraft-independent and testable with deterministic fake metrics. Rendering receives already-positioned baseline-aware boxes. The model adds a small amount of layout-specific vocabulary, but avoids pushing math rules into document layout or Minecraft rendering.
