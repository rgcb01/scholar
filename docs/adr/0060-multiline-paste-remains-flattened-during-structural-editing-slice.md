# 0060 Multiline Paste Remains Flattened During Structural Editing Slice

Status: Accepted

## Context

Multi-block copy now emits newlines, but structural multiline paste has not been designed.

## Decision

Paste continues to normalize `\r\n`, `\r`, and `\n` to spaces before replacement, including when replacing a multi-block selection.

## Alternatives Considered

- Convert pasted newlines into new document blocks now.
- Preserve literal newline characters in `Text`.
- Reject multiline paste over multi-block selections.

## Consequences

Paste remains safe and consistent with previous single-block behavior. Copy/paste of a multi-block selection is not yet a structural round trip.
