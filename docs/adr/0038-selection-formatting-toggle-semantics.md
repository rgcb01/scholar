# 0038 Selection Formatting Toggle Semantics

Status: Accepted

## Context

Scholar now supports semantic inline marks for selected text. A toggle command must behave predictably for selections that are unmarked, fully marked, or partially marked.

## Decision

For a selected range and one `TextMark`, if every selected user-character already has the mark, the command removes that mark from the whole selection. Otherwise, the command applies the mark to the whole selection.

## Alternatives Considered

- Toggle each marked run independently.
- Preserve mixed state and do nothing until the user chooses an explicit add/remove command.

## Consequences

Mixed selections resolve to ON. The rule matches common editor behavior and keeps one user command equivalent to one undoable document edit.
