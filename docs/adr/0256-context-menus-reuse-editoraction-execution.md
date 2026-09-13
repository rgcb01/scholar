# ADR 0256 - Context Menus Reuse EditorAction Execution

## Status

Accepted

## Context

Scholar already has shared actions for menu-bar, toolbar, and keyboard shortcut execution. Adding right-click menus could either reuse that layer or create context-menu-specific editing paths.

## Decision

Context menus resolve and execute existing `EditorAction` instances. The resolver receives the current action registry and skips unavailable actions rather than constructing independent commands.

## Alternatives Considered

- Implement context-menu-specific editing callbacks. This would duplicate enablement and history behavior.
- Hard-code direct `EditorSession` mutations in the widget. This would bypass shared action guards.

## Consequences

Right-click behavior stays aligned with menus, toolbar, shortcuts, clipboard side effects, undo/redo, and action enablement. The context menu can evolve by adding or removing registered actions without creating another editing API.
