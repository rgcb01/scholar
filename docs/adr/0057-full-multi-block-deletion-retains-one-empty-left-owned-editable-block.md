# 0057 Full Multi-Block Deletion Retains One Empty Left-Owned Editable Block

Status: Accepted

## Context

A multi-block selection can cover all text in every selected editable block. The editor still needs a valid insertion host after deletion.

## Decision

Deleting a complete multi-block range leaves one editable block with empty `InlineContent`, owned by the left endpoint block style.

## Alternatives Considered

- Produce an empty document region with no block.
- Inherit the right block style when no left text remains.
- Insert a fake empty `Text` node.

## Consequences

Caret placement remains valid and no `Text("")` nodes are required. Left ownership remains consistent with boundary deletion.
