# 0097 - External Plain-Text Math Paste Uses A Bounded Lexical Importer

## Status

Accepted

## Context

Scholar needs to paste ordinary external math text into equations without turning the editor into a parser, CAS, or LaTeX system.

## Decision

External plain-text math paste uses a bounded lexical importer that produces a linear `MathSequence` of authored atoms.

## Alternatives Considered

- Parse expressions into precedence trees.
- Reuse Markdown or LaTeX import.
- Reject all external math text until a richer parser exists.

## Consequences

Common text paste becomes useful while keeping semantics predictable. The importer does not validate mathematical correctness or infer structure.
