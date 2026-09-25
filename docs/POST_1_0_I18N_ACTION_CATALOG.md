# Post-1.0 UI internationalization and action catalog

## Boundaries

Scholar UI chrome is localized through `assets/scholar/lang/en_us.json` and
`es_mx.json`. `ScholarText` creates Minecraft translatable components, while
`ScholarTranslations` resolves plain strings for layout and pure model code.
Dynamic messages use positional placeholders so languages can reorder values.

`BuiltInEditorActionCatalog` is the single immutable source for an action's
translation key, optional tooltip key, icon identity, category, and shortcut.
`EditorAction` keeps its compatible raw-label API and exposes catalog metadata
through additive default methods. Execution, enablement, domain validation, and
ribbon composition remain Java behavior.

## String classification

- Localized: menus, actions, tooltips, dialogs, status, home, templates,
  import/export, and dataset, plot, figure, table, diagram, and document chrome.
- Literal scientific content: equations, variable names, values, unit symbols,
  notation, and generated scientific document text.
- Stable literal identity: filenames, IDs, serialization values, enum values,
  and persistence schema data.
- Literal diagnostics: exception details and developer/debug messages.
- Literal authored content: all user-provided document and dataset text.

The focused hardcode boundary test permits these semantic categories and guards
obvious English literals in production screen and UI rendering code.

## Next cleanup

1. Split remaining screen composition into smaller view models so more layout
   can be tested without Minecraft classes.
2. Add a dedicated template catalog later; keep template construction separate
   from localized names and descriptions.
3. Add screenshot-based locale QA for narrow widths to detect clipping as the
   vocabulary grows.
4. Introduce translation-key wrappers only if key families become large enough
   that raw strings cause mistakes; do not move behavior or scientific policy
   into JSON.

## Manual QA

Run once with `en_us` and once with `es_mx`:

- Open Home, select each template, and create a document.
- Inspect every ribbon tab, toolbar, context menu, dialog, and status item.
- Exercise Save, Save As, Undo, Redo, shortcuts, and document management.
- Exercise dataset interaction, scientific insertion, plots, figures, tables,
  diagrams, and import/export.
- Check for literal translation keys, mixed languages, clipping, overlaps,
  incorrect accents or symbols, and label drift between action surfaces.
- Confirm user text, filenames, IDs, equations, values, and unit symbols remain
  unchanged when switching locale.
