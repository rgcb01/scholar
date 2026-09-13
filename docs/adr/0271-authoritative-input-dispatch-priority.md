# ADR 0271 - Authoritative Input Dispatch Priority

## Status

Accepted

## Context

Scholar now has document text, structured math, tables, plots, diagrams, figures, context menus, toolbar menus, menu-bar popups, and modal data-entry popups. Without a stable dispatch order, one physical input event can accidentally affect multiple layers.

## Decision

Input dispatch uses a fixed priority: modal popups, context menu, open menu/toolbar popup, active nested editor, document editor, then screen fallback.

## Alternatives Considered

- Let each component opportunistically handle input independently.
- Route all input through a large generic dispatcher before any UI component sees it.

## Consequences

The screen remains the Minecraft boundary for physical events, while lower-priority consumers are isolated from events already owned by a higher-priority layer. Future UI layers must choose an explicit position in the priority order.

