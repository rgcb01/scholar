# ADR 0225: FigureBlock wraps existing scientific visual blocks

## Status
Accepted for M20.

## Context
Scholar already has semantic `PlotBlock` and `DiagramBlock` document blocks. Figures need captions and numbering without replacing those existing visual models or introducing image/media support prematurely.

## Decision
Represent figures as `FigureBlock(id, content, caption)` where `content` is currently limited to an existing `PlotBlock` or `DiagramBlock`.

## Alternatives Considered
- Add captions directly to every visual block type.
- Create separate `PlotFigureBlock` and `DiagramFigureBlock` records.
- Introduce a broad media hierarchy before images, tables, and future content require it.

## Consequences
Plot and diagram ASTs remain reusable both inside and outside figures. Figure behavior can be added at the document/layout/editor boundary while future media types can be admitted deliberately.
