# 0056 Multi-Block Range Replacement Collapses Into Left Block

Status: Accepted

## Context

Scholar now supports multi-block selections across adjacent editable text blocks. Structural mutation needs one deterministic rule for delete, cut, typing replacement, paste replacement, and Enter preparation.

## Decision

Replacing a multi-block range keeps the prefix of the start block, inserts optional replacement text, appends the suffix of the end block, and collapses the result into the start block. The start block's semantic type and heading level own the result.

## Alternatives Considered

- Choose the resulting block style from the end block.
- Dynamically choose a style based on remaining text.
- Delete blocks first and then reinsert through Markdown.

## Consequences

Backspace, Delete, Cut, Paste, and typing share one predictable AST transformation. Some edge cases intentionally preserve left block style even when all original left text is removed.
