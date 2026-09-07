# 0045 Single Active Shell Popup Policy

Status: Accepted

## Context

Scholar now has top menu dropdowns and a toolbar block style dropdown. Independent popups can overlap and leak input into the document if not coordinated.

## Decision

Only one shell popup may be open at a time. Opening a menu closes the toolbar block style dropdown, and opening the toolbar dropdown closes top menus. Escape and outside clicks close active popups before document input is processed.

## Alternatives Considered

- Let each widget own independent popup state.
- Build a full global popup manager.
- Disable toolbar popups while menus exist.

## Consequences

Input routing remains deterministic without a large UI framework. Future shell popups can follow the same coordination rule.
