# ADR 0260 - Context Menus Cannot Bypass Action Enable Guards

## Status

Accepted

## Context

Some context menus intentionally include relevant-but-currently-disabled actions, such as table row operations on dataset-backed tables or object actions for a target that cannot currently perform a mutation.

## Decision

Context menu rows render disabled when their `EditorAction.isEnabled(...)` predicate is false, and disabled rows do not execute. The widget never calls lower-level editor mutation methods directly.

## Alternatives Considered

- Hide every disabled action. This can make menus feel unstable and hide useful contextual affordances.
- Execute disabled actions and let lower layers no-op. This would blur action applicability and user feedback.

## Consequences

The same enablement contract applies from shortcuts, toolbar, menu bar, and right-click. Dataset-backed table restrictions, editing-mode restrictions, and future validation guards remain centralized.
