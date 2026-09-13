# ADR 0259 - Context Menus Are Transient UI State

## Status

Accepted

## Context

Context menus should not become document state, history state, or persistent editor mode. They also must not conflict with toolbar dropdowns, menu-bar popups, or modal editing popups.

## Decision

The open context menu lives only in `ScholarEditorScreen`. It closes on Escape, outside click, resize, incompatible popup opening, and successful action activation.

## Alternatives Considered

- Store context menu state in `EditorState`. This would pollute semantic state with client presentation.
- Keep menus open across layout changes. This risks stale geometry and stale action relevance.

## Consequences

Context menus remain a lightweight Minecraft shell surface. Document/editor history stays focused on semantic mutations rather than UI chrome.
