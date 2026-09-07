# 0034 Shared Editor Action Abstraction

Status: Accepted

## Context

Undo, redo, cut, copy, and paste can be invoked from keyboard shortcuts today and from menus or toolbars later. The same command must not be reimplemented in each UI surface.

## Decision

Introduce a small Minecraft-independent `EditorAction` abstraction with stable `EditorActionId` values, labels, shortcut display metadata, enabled-state checks, and execution through `EditorActionContext`.

## Alternatives Considered

- Keep helper methods directly on `ScholarEditorScreen`: rejected because menus and future toolbars would duplicate command logic.
- Build a full command bus or action framework: rejected as too large for the current editor.
- Make actions execute against Minecraft screens: rejected because logical editor actions should remain renderer independent.

## Consequences

Keyboard shortcuts and menu items can invoke the same action objects. Future toolbar/context-menu integration has a small reusable command surface without committing to a broad framework.
