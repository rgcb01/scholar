# M19B.1 — Scrollable Dropdown Menus

Long menu dropdowns are now constrained to the current screen height instead of rendering below the viewport.

- The menu calculates a visible row window from the actual screen height.
- Mouse-wheel scrolling over an open dropdown moves that row window.
- Hit-testing maps visible rows back to the correct underlying menu entry.
- Opening another menu resets its scroll position.
- Closing a menu resets scroll state.
- Small up/down indicators communicate hidden content.
- The behavior is generic for every Scholar menu, not Diagram-specific.

No Diagram, electrical, mechanical, document, clipboard, or history semantics changed.
