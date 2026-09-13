# ADR 0240: Structure Navigation Targets Stable Heading IDs

## Status

Accepted

## Context

TOC and outline navigation must survive renumbering and heading text edits. Block indices are convenient during layout but are not stable document identities.

## Decision

Navigation entries target stable heading IDs. The current resolver maps those IDs to block indices at navigation time. Pasting a heading with an existing ID remaps the pasted heading ID to avoid duplicate targets.

## Alternatives Considered

- Navigate by current block index only. This is fragile across edits and clipboard operations.
- Use heading text as identity. Text is user-editable and not unique.
- Introduce persistent IDs for every node. This broadens the identity model beyond M22.

## Consequences

Navigation remains stable for intended heading targets while the document model avoids universal node identity. Heading clipboard paste must remap duplicate IDs.
