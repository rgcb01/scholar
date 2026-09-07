# 0071 - Structural Object Deletion Uses Adjacent Selection Recovery With Left Bias

Status: Accepted

## Context

Deleting a selected object must leave the editor in a valid, predictable selection state.

## Decision

After deletion, Scholar prefers the previous editable text block end, then previous object, then next editable text block start, then next object. If no blocks remain, it creates one empty Paragraph and places the caret at offset 0.

## Alternatives Considered

- Always select the next block. This makes deletion feel less stable when a previous authoring position exists.
- Leave no selection. The editor requires one active selection mode.

## Consequences

Deletion supports adjacent objects and only-object documents while preserving a usable authoring state.
