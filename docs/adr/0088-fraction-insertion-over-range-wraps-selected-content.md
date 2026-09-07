# 0088 - Fraction Insertion Over Range Wraps Selected Content

## Status

Accepted

## Context

The Fraction action already inserted an empty fraction at a caret. With math selection, invoking Fraction over existing content needs a structural replacement behavior.

## Decision

When a valid same-sequence math range is selected, Fraction replaces the selected content with a new `MathFraction`. The selected content becomes the numerator as a `MathSequence`, the denominator starts empty, and the caret moves to the denominator.

## Alternatives Considered

- Delete the range and insert an empty fraction.
- Put selected content in the denominator.

## Consequences

The action supports a predictable wrap workflow while preserving the caret-only fraction insertion behavior.
