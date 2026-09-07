# 0120 - Caret Script Characters Are Explicit Structural Authoring Commands

## Status

Accepted

## Context

Scholar avoids silent semantic inference, but `^` and `_` are familiar direct authoring gestures inside equation editing.

## Decision

In equation editing, typed `^` and `_` are explicit structural commands for superscript and subscript slots. They are not LaTeX parsing and external paste does not infer scripts from plain text.

## Alternatives Considered

- Keep `^` and `_` as no-ops.
- Insert literal symbol nodes.
- Add a LaTeX-like parser.

## Consequences

Keyboard and explicit actions can share one core editor operation. Ordinary external text remains lexical and non-structural.
