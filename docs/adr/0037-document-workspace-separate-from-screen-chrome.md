# 0037 Document Workspace Separate From Screen Chrome

Status: Accepted

## Context

The editor screen needs top-level shell chrome now and will later need sidebars, status bars, and reader/editor mode changes. The document viewport should not assume the full screen is available.

## Decision

Treat the document workspace as a bounded region below shell chrome. The menu bar consumes vertical screen space, and document layout/rendering use the remaining viewport.

## Alternatives Considered

- Draw shell chrome over the existing document viewport: rejected because it causes input and clipping ambiguity.
- Keep one full-screen document area and patch offsets ad hoc: rejected because future sidebars/status bars would require rewrites.
- Build the full workspace/sidebar system immediately: rejected as beyond the first shell slice.

## Consequences

Menu chrome stays fixed while the document scrolls beneath it. Future sidebar and status bar regions can be added by changing workspace layout instead of document rendering semantics.
