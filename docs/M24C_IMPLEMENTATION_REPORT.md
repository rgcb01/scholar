# M24C Implementation Report

Status: implemented; automated validation complete.

M24C adds the first editor-wide right-click context menu foundation.

## Implemented

- Pure Java `EditorContextActionResolver` maps semantic `EditorState` selections to relevant existing `EditorAction`s.
- Context menu entries are represented by small immutable value types.
- Minecraft client `ContextMenuWidget` renders a shell-native popup with disabled action rows, shortcut labels, pointer placement, viewport clamping, keyboard navigation, and scroll support.
- `ScholarEditorScreen` opens the context menu from right-click, updates selection according to the M24B selection contract, and closes incompatible transient UI.
- Empty document area right-click preserves selection and exposes only empty-area actions.
- Missing actions are skipped during context construction.
- Context menu activation routes through the shared controller `execute(EditorAction)` path.

## Not Implemented

- Nested context-menu submenus.
- New edit commands that do not already exist as `EditorAction`s.
- Object-level document clipboard beyond existing block/table/plot/diagram/figure behavior.

## Validation

- `EditorContextActionResolverTest` covers text, heading, equation, table, plot, diagram, figure, table-of-contents, empty-area, action reuse, separator cleanup, and missing-action behavior.
- `ContextMenuLayoutTest` covers pointer placement, viewport clamping, tiny viewports, and long-menu row caps.
- Full Gradle test/build validation is recorded in the milestone completion report.
