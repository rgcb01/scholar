# Post-1.0 scientific-instrument UI

## Visual boundary

Scholar uses a compact scientific-instrument language for application chrome:
graphite/navy surfaces, restrained cyan state indicators, thin technical
borders, fixed integer geometry, and three depth levels. Normal actions inherit
their shared panel instead of appearing as permanently framed buttons.

The white publication page remains outside this system. Document typography,
paper color, equations, tables, figures, plots, pagination, and PDF output are
unchanged.

## Rejected-pass audit

Retained:

- the pure shared control-state model;
- reusable shell rendering entry points;
- localized, wrapped ribbon and toolbar tooltips;
- dialog, menu, Home, and status-bar integration points.

Replaced:

- raised/recessed bevels and content shifting;
- permanent boxes around ribbon and toolbar actions;
- framed ribbon groups and inventory-like cards;
- thick popup, tooltip, dialog, slider, and tab treatments.

ScholarShellStyle is the only shell token source. ScholarShellRenderer owns the
flat panel, action, group, tab, popup, menu-row, dialog, tooltip, and slider
primitives. ScholarButton applies the same state model to Home and dialog
actions.

## Typography decision

Scholar's bundled Source Sans 3/STIX font registrations are currently routed
through document components, not exposed as an independent shell Font. This
pass keeps Minecraft's GUI font for application chrome to preserve Unicode and
locale behavior rather than introducing a second font loader. The document's
readable scientific typography remains untouched. A dedicated shell-font
resource is the remaining visual follow-up.

## Manual visual QA

Run this matrix in en_us and es_mx, at a narrow supported window, a typical
16:9 window, a 1920x1080-like workspace, a larger window, and relevant GUI
scales.

1. Home: header, document/template cards, paging, hover/focus, and context menu.
2. File and Home ribbon: normal, hover, pressed, selected/mixed, disabled, and
   keyboard-focus states; active/inactive tabs and group separators.
3. Insert, Data, Figure, Diagram, Layout, and View: overflow, compression,
   icon alignment, Spanish labels, palettes, and dropdown placement.
4. Context menus and dropdowns: hover rows, selected marks, disabled text,
   separators, hitboxes, and viewport clamping.
5. Dialogs and inputs: hierarchy, action area, focus, long scientific values,
   selectors, lists, and validation text.
6. Tooltips: localized wrapping, shortcut lines, compact frame, and boundaries.
7. Status bar and zoom: segments, fit controls, track, active portion, handle,
   percentage, narrow-width collisions, and tooltip.
8. Scrollable lists: integrated narrow scrollbar, contrast, hover, and dragging.
9. Workspace: breathing room and white paper contrast; verify that document
   content and scientific rendering are visually unchanged.

Owner inspection remains the acceptance gate.
