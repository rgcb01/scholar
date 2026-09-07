# 0033 Multiline Paste Policy

Status: Accepted

## Context

Scholar does not yet support Enter, paragraph creation, paragraph splitting, or cross-paragraph editing, but users may paste text containing line breaks.

## Decision

For the first clipboard slice, normalize `\r\n`, `\n`, and `\r` to spaces before inserting pasted plain text.

## Alternatives Considered

- Reject multiline paste: safe, but less useful when pasting ordinary copied text.
- Create new paragraphs: rejected because paragraph creation and block splitting are out of scope.
- Preserve raw line break characters inside `Text`: rejected because the editor has no rendering or editing semantics for line breaks yet.

## Consequences

Multiline paste is deterministic and does not invent document structure. This policy is temporary and can be revisited when paragraph editing is designed.
