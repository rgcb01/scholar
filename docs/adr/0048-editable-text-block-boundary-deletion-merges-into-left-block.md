# 0048 Editable Text Block Boundary Deletion Merges Into Left Block

Status: Accepted

## Context

Backspace and Delete at boundaries between editable text blocks need deterministic behavior across paragraphs and headings.

## Decision

Deleting a boundary between two editable inline text blocks concatenates their inline content into the left block. The left block's semantic style wins, including heading level.

## Alternatives Considered

- Preserve the right block style.
- Choose style based on the key used.
- Refuse to merge different block styles.

## Consequences

Backspace from the start of the right block and Delete from the end of the left block produce the same result. Unsupported blocks such as equations are not deleted or merged by this boundary operation.
