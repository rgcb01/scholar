# 0041 Rich Text Normalization Deferred

Status: Accepted

## Context

Formatting transformations can leave adjacent `Text` nodes with identical marks. Earlier document model work intentionally avoided silent normalization because cursor, history, and editor transformations were still developing.

## Decision

Do not automatically merge adjacent equal-mark `Text` nodes during rich-text formatting, typing, or serialization.

## Alternatives Considered

- Normalize after every formatting command.
- Normalize during document construction.
- Normalize during Markdown serialization.

## Consequences

Formatting preserves existing segmentation unless a command explicitly splits text. A future explicit normalization pass can be designed with documented cursor/history behavior.
