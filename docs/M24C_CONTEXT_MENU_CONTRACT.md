# M24C Context Menu Contract

Status: Accepted and implemented.

M24C adds a Minecraft-native right-click context menu layer for the Scholar editor. The menu is a transient UI surface that resolves relevant existing `EditorAction`s from the current semantic editor state.

## Core Rules

- Context menus execute existing `EditorAction` instances only.
- Context menus do not define duplicate editing methods.
- Missing registered actions are skipped while building a menu.
- Disabled actions remain disabled when reached from the context menu.
- Opening a context menu closes menu-bar and toolbar popups.
- Opening another modal popup closes the context menu.
- Resize closes the context menu.
- Escape closes the context menu.
- Clicking outside closes the context menu.

## Right-Click Selection Policy

- Right-click inside the current selected text or table-cell text preserves that selection.
- Right-click outside the current selection selects the clicked semantic target first.
- Right-click on empty document space leaves the editor state unchanged and shows only empty-area actions, currently Paste when applicable.
- Cross-reference labels and other derived inline text are reached through the document hit tester; the menu acts on the underlying semantic inline unit, not on fake editable label characters.

## Action Relevance

- Text selections expose clipboard, inline formatting, and cross-reference insertion actions.
- Heading text selections also expose block-style actions.
- Figure captions expose text clipboard and inline formatting actions.
- Equation editing exposes math authoring actions.
- Table-cell editing exposes cell clipboard/formatting plus row and column actions.
- Plot editing exposes plot actions.
- Diagram editing exposes target-relevant diagram actions.
- Whole block selections expose clipboard plus block-specific existing actions where relevant.
- Empty space exposes Paste only.

## UI Behavior

- The widget opens near the pointer and clamps to the current viewport.
- Long menus scroll instead of extending beyond the viewport.
- Disabled rows render visibly disabled and cannot execute.
- Keyboard navigation supports Up, Down, Enter, and Escape.
- The first M24C widget does not implement nested submenus; long diagram menus are kept bounded by scrolling and semantic filtering.
