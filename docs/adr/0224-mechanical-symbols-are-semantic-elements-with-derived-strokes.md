# ADR 0224: Mechanical symbols are semantic elements with derived strokes

## Status
Accepted for M19D.

## Decision
Represent reusable mechanical drawing symbols as `MechanicalSymbol` diagram elements carrying a stable kind and authored bounds. Render shaft, gear, bearing, spring, piston, and bolt geometry programmatically from those bounds.

## Consequences
The document stores mechanical meaning rather than renderer pixels; symbols participate in the existing diagram editing, history, clipboard, zoom/pan, and hit-testing infrastructure; future symbol metadata can extend the model without replacing the rendering contract.
