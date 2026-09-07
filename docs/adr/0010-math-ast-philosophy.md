# ADR 0010 - Math AST Philosophy

## Status

Accepted

## Context

Scholar needs to represent scientific equations in documents, but it is not intended to be TeX, LaTeX, a computer algebra system, or an equation solver. The model must preserve authored notation while staying useful for future layout, rendering, and editing.

## Decision

Scholar Math uses a pragmatic presentation-semantic hybrid AST. It represents authored mathematical notation structurally, such as fractions, scripts, roots, groups, symbols, identifiers, and operators, without trying to model executable mathematical meaning.

## Alternatives Considered

- Store equations as raw LaTeX-like strings.
- Build a fully semantic expression tree such as divide, add, solve, and evaluate nodes.
- Defer a math model until rendering exists.

## Consequences

Scholar can represent educational notation faithfully without committing to evaluation or simplification. Layout and editing can work from structured nodes. Some mathematical meaning, such as operator precedence or function resolution, remains outside the v0.1 AST.
