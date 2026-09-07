# 0064 - Boundary-Only Selection Applies To Block Style But Not Inline Formatting

Status: Accepted

## Context

A selection from the end of one editable block to the start of the next selects a structural boundary but no text characters.

## Decision

Boundary-only selections are NOT_APPLICABLE for inline Bold and Italic. The same selection touches both blocks for block style queries and commands.

## Alternatives Considered

- Make both domains treat boundary-only selections the same way. This would blur the distinction between character formatting and structural formatting.

## Consequences

Bold and Italic are disabled for boundary-only selections, while Paragraph and Heading commands remain enabled and target both adjacent editable blocks.
