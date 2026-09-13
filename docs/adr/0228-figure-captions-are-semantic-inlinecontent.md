# ADR 0228: Figure captions are semantic InlineContent

## Status
Accepted for M20.

## Context
Captions need to be editable text attached to the figure, not renderer-only labels.

## Decision
Store a figure caption as `InlineContent`. The layout/rendering boundary prepends the generated bold `Figure N.` prefix; the prefix is not authored caption content.

## Alternatives Considered
- Store captions as plain strings.
- Store generated "Figure N." text inside the caption itself.
- Treat captions as separate paragraph blocks after visual content.

## Consequences
Caption content remains part of the immutable document AST and can later support richer inline marks. Generated numbering remains consistent and cannot be edited accidentally as authored caption text.
