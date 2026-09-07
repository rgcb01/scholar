# 0101 - External Math Import Is Atomic And Rejects Unsupported Characters

## Status

Accepted

## Context

Clipboard text is untrusted and may contain syntax Scholar does not understand yet.

## Decision

External math import succeeds only when the entire bounded input can be tokenized. Unsupported characters or unsupported syntax fail the whole import.

## Alternatives Considered

- Drop unsupported characters and paste the rest.
- Stop parsing at the first unsupported character and paste a prefix.
- Substitute placeholders for unknown content.

## Consequences

Scholar never silently loses clipboard content. Invalid paste is non-destructive and can later gain user-facing error feedback.
