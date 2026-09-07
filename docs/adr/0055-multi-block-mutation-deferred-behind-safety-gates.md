# 0055 Multi-Block Mutation Deferred Behind Safety Gates

Status: Accepted

## Context

Milestone 10B proves multi-block selection representation, geometry, and copy before implementing destructive multi-block edits.

## Decision

Multi-block mutation commands are temporarily gated. Cut, paste replacement, typing replacement, Enter, Backspace/Delete, inline formatting, and block-style changes do not mutate multi-block selections in 10B. Same-block behavior remains unchanged.

## Alternatives Considered

- Implement all multi-block mutations immediately.
- Let existing single-block mutation code run against one endpoint.
- Collapse multi-block selections before mutation.

## Consequences

Selection can be validated safely without partial document corruption. Later milestones can implement deletion/replacement and formatting deliberately.
