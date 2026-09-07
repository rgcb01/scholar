# ADR 0009 - Continuous Scrolling for Initial Viewer

## Status

Accepted

## Context

The first read-only viewer needs to display documents that exceed screen height. Pagination, page turning, zoom, and document navigation would add UI decisions beyond the first vertical slice.

## Decision

The initial Scholar viewer uses continuous vertical scrolling with clamped scroll range. Scroll offset belongs to screen/UI state and is applied during rendering; laid-out document positions remain stable document-space coordinates.

## Alternatives Considered

- Pagination.
- Horizontal scrolling.
- Book-style page turning.
- Recomputing layout positions when scrolling.

## Consequences

The first viewer is simple to use and test manually. It does not yet provide page navigation or advanced reading controls.
