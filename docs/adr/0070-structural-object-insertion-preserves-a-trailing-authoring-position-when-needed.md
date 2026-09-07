# 0070 - Structural Object Insertion Preserves A Trailing Authoring Position When Needed

Status: Accepted

## Context

Inserting an object at the end of a document could leave no convenient place to continue typing.

## Decision

When an inserted structural object becomes the final document block, Scholar appends an empty Paragraph with empty InlineContent.

## Alternatives Considered

- Add a leading empty Paragraph. This creates unnecessary content before objects.
- Persist fake text or fake math content. This pollutes semantic document data.

## Consequences

Users can continue authoring after final inserted objects, and empty equations remain semantically empty.
