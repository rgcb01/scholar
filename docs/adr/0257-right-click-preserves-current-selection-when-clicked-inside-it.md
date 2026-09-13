# ADR 0257 - Right Click Preserves Current Selection When Clicked Inside It

## Status

Accepted

## Context

Context menus are commonly used to act on an existing selection. Replacing the selection before opening the menu would make Cut, Copy, formatting, and block operations act on the wrong target.

## Decision

Right-click inside the current selected text or table-cell text preserves that selection. Right-click outside the active selection selects the clicked semantic target before resolving the menu.

## Alternatives Considered

- Always collapse to the clicked point. This would prevent context actions on an existing range.
- Never adjust selection on right-click. This would make context menus for unselected targets feel disconnected from the clicked object.

## Consequences

Existing selections remain actionable, while unselected content can still be targeted directly with one right-click. The rule follows M24B selection validity and does not introduce new selection shapes.
