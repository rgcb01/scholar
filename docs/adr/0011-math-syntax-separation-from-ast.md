# ADR 0011 - Math Syntax Separation From AST

## Status

Accepted

## Context

Scholar may eventually support LaTeX-like input, visual equation editing, Markdown math syntax, and canonical serialization. These input and output forms should not become the internal math source of truth.

## Decision

The Scholar Math AST contains structured mathematical nodes, not LaTeX command names or stored syntax strings. Future parsers may translate syntax such as `\frac{d}{t}` or `\Delta` into Math AST nodes.

## Alternatives Considered

- Store the original authoring syntax directly in the document model.
- Treat LaTeX-like commands as the semantic representation.
- Implement syntax and AST simultaneously.

## Consequences

Different authoring surfaces can produce the same Math AST. Future syntax can evolve without rewriting the core math model. The project must later define explicit parser and serializer boundaries.
