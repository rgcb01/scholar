# 0103 - Ordinary Math Letter Authoring Uses Individual Identifier Atoms

## Status

Accepted

## Context

ADR 0100 chose to import contiguous alphabetic runs as named identifiers. That made pasted `xy` become `MathIdentifier("xy")`, which is closer to programming-language identifiers than conventional mathematical notation. Scholar needs to preserve authored notation without inferring multiplication, function semantics, or descriptive text semantics.

## Decision

Ordinary unstructured mathematical letter authoring uses individual `MathIdentifier` atoms. Typing or importing `xy` creates adjacent `MathIdentifier("x")` and `MathIdentifier("y")`, and typing or importing `velocity` creates adjacent single-letter identifier atoms. Scholar does not insert an implicit multiplication operator.

This supersedes ADR 0100 for default keyboard authoring and external plain-text import.

## Alternatives Considered

- Keep contiguous alphabetic runs as one `MathIdentifier`.
- Insert explicit multiplication between adjacent letters.
- Infer named operators such as `sin` or named quantities such as `velocity`.

## Consequences

Ordinary notation such as `xy`, `ab`, and `Δx` has a stronger mathematical editing foundation, especially for future scripts and structured export. Named operators, descriptive math text, and named quantities will require explicit future semantics instead of heuristic keyword or word recognition.
