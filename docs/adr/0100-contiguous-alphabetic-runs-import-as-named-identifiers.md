# 0100 - Contiguous Alphabetic Runs Import As Named Identifiers

## Status

Superseded by [0103 Ordinary Math Letter Authoring Uses Individual Identifier Atoms](0103-ordinary-math-letter-authoring-uses-individual-identifier-atoms.md)

## Context

External STEM text often contains named quantities such as `velocity`, `temperature`, and `mass`. Splitting pasted words into individual variables would be noisy and less useful for document authoring.

## Decision

Contiguous Unicode letter runs import as one `MathIdentifier`, preserving case and authored Unicode.

## Alternatives Considered

- Split every letter into its own identifier atom.
- Infer implicit multiplication from adjacent letters.
- Use contextual function or unit recognition.

## Consequences

Named quantities paste naturally. Ambiguous notation such as `xy` becomes a named identifier rather than multiplication; users can write `x*y` when they want an explicit operator.
