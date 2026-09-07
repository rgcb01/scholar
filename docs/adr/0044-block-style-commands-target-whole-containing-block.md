# 0044 Block Style Commands Target Whole Containing Block

Status: Accepted

## Context

The current selection model is single-block, and block styles apply to document structure rather than individual characters.

## Decision

A block style command applies to the entire containing editable text block, regardless of whether the caret is collapsed or a subrange of text is selected.

## Alternatives Considered

- Apply heading style only to selected text.
- Require a full-block selection before changing block style.
- Delay block style editing until multi-block selections exist.

## Consequences

The behavior matches common editors and keeps `DocumentPosition` unchanged. Cross-block mixed style state remains future work.
