# Editor Architecture

This document describes runtime ownership in the Scholar editor. It is an
implementation map, not a public addon API contract.

## Dependency Direction

```text
ScholarEditorScreen
  -> presentation controllers and components
    -> EditorSession public commands
      -> cohesive session command collaborators
        -> document, editor, and scientific domain services
```

Dependencies point inward. Domain and command code does not depend on the
Minecraft screen, translated labels, widgets, or rendering types. Visible
labels, icons, and shortcuts continue to come from the unified built-in action
catalog.

## Screen Ownership

`ScholarEditorScreen` is the Minecraft `Screen` composition root. It owns the
screen lifecycle, dimensions and high-level layout, render order, current
workspace/session reference, high-level focus, document viewport geometry,
selection/caret interaction, and composition of the accepted Scientific
Instrument UI components.

Presentation responsibilities are delegated as follows:

| Collaborator | Ownership |
| --- | --- |
| `EditorDialogCoordinator` | Exclusive modal identity and open/close state. Dialog field values remain presentation state; confirmed values are sent to session commands. |
| `EditorInputDispatcher` | Ordered key-route short-circuiting. Routes translate an event to an existing UI intent or action and contain no document mutation. |
| `EditorContextMenuFactory` | Context-action resolution and `ContextMenuWidget` construction from the unified action set. |
| `EditorStatusBar` | Word-count cache, status rendering, status hit testing, and zoom-slider interaction. |
| `EditorOutlinePanel` | Outline open/scroll state, structure rendering, heading hit testing, and navigation callbacks. |

The screen still contains document rendering and selection hit testing because
they share the live viewport transform and Minecraft render lifecycle. Dialog
body rendering and field editing also remain in the screen for now; their
modal ownership is centralized, and their confirmed mutations do not bypass
the session.

## Session Ownership

`EditorSession` remains the authoritative owner and coordinator for:

- current immutable `EditorState`, including document, caret, selection, and
  active nested editor selection;
- `EditorHistory`, undo/redo, typing coalescing, and transaction depth;
- general text, math, table, plot, figure, clipboard, selection, and navigation
  operations;
- validation-compatible document replacement and transient edit lifecycle;
- the stable public command surface used by controllers and addons.

Large domain command families are delegated internally:

| Collaborator | Ownership |
| --- | --- |
| `ScientificInsertionCommands` | Variables, computed results, dataset analyses, fit overlays, expression binding, and computation snapshots. |
| `DatasetSessionCommands` | Dataset creation/copy/removal, row/column/cell mutation, units, dataset-backed tables, and plot bindings. |
| `DiagramSessionCommands` | Electrical component/junction operations, connections, and mechanical constraint operations plus their transient drafts. |

These collaborators receive the owning session explicitly. Public
`EditorSession` methods preserve their signatures and delegate to the relevant
command family, so callers do not acquire a second state owner.

## State And History Boundary

There is one owner for each durable editing concern:

- document, selection, caret, and history: `EditorSession`;
- persisted dirty/save baseline: the document workspace/application layer;
- modal and view state: presentation collaborators;
- diagram connection and mechanical-constraint drafts: `DiagramSessionCommands`.

Command collaborators produce an `EditResult` or domain-specific diagram edit
result and return it to the owning session. The session applies it through the
existing `EditorHistory.applyEdit` boundary. A successful logical command
therefore remains one undo transaction; navigation, previews, cancelled
drafts, and no-ops remain outside history. Workspace dirty state continues to
observe session document changes through the existing document-change path.
Future autosave or recovery can observe that same path without depending on
`ScholarEditorScreen`; neither feature is implemented here.

## UI And Domain Boundary

Presentation code gathers and validates user input, then invokes commands with
typed values, stable IDs, and domain enums. It does not mutate deep document
structures directly. Command collaborators never inspect localized text and do
not render UI. Persistence and transfer continue to serialize semantic
documents only; runtime collaborators and their transient state are not
serialized.
