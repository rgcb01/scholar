# Scholar

Scholar is a scientific document editor and document engine inside Minecraft Java Edition. It lets players create, edit, save, and read structured scientific work in the game. The engine is intended to support future educational and scientific addons, but it is not an experiment pack, a set of STEM minigames, a Minecraft Word clone, or a notebook programming environment.

## What Scholar Is

Scholar keeps a semantic document model as its source of truth. Layout, reference labels, computations, analysis results, and rendering are derived from that model. The application shell stays recognizable as Minecraft, while the document surface prioritizes scientific readability. Scholar is independent of any future Physics/STEM Lab mod.

## Current Capabilities

- Structured prose, headings, document outline, table of contents, and cross-references.
- Scientific typography, equations and structured math, physical page settings, one- and two-column typesetting, and templates.
- Editable tables, reusable datasets, static and dataset-backed plots, electrical and mechanical diagrams, figures, and captions.
- Structured units and quantities, named variables, computed results, dataset statistics, polynomial fits, and derived plot overlays.
- Document Home, multi-document workspaces, save/open/rename/delete, undo/redo, and semantic clipboard transfer with plain-text fallbacks.
- High-fidelity document text and stroke rendering with zoom, fit controls, caret, selection, and hit testing.

## Screenshots

Project screenshots have not been checked into the repository yet. Use `/scholar` and the **M34 Readability Sample** template to inspect the current document surface in-game.

## Getting Started

Scholar targets Minecraft Java Edition `1.21.1`, NeoForge `21.1.249`, and Java `21`. The repository uses the Gradle `9.2.1` wrapper.

From the repository root on Windows:

```powershell
.\gradlew.bat runClient
.\gradlew.bat test
.\gradlew.bat build
```

On Unix-like systems use `./gradlew` with the same tasks. The built mod JAR is written to `build/libs/` (currently `scholar-0.1.0.jar`). Gradle downloads the development dependencies on the first run.

## Using Scholar

Enter a Minecraft world and run `/scholar`. Home lets you create a blank or template document, open an existing one, and manage saved documents. Edit in the document workspace and use File > Save; closing a modified document prompts you to save, discard, or cancel. The M34 Readability Sample is an optional production template, not an automatically created file.

## Architecture

The semantic `Document` and its resources are canonical. Validation and editor transactions preserve invariants; layout derives pages, columns, geometry and labels; the Minecraft client renders that layout. Transfer and versioned persistence are separate boundaries, neither of which serializes transient view state. See [Architecture](docs/ARCHITECTURE.md) for the current layer map and ownership rules.

There is **no stable public addon API yet**. M35 is intended to define that boundary; current internal Java packages should not be treated as extension contracts.

## File Format

Scholar saves local `.scholar.json` files using its versioned semantic JSON codec. Current writes use V2; supported V1 files load with deterministic defaults. The file stores authored content and stable semantic IDs, not selection, undo history, zoom, cached computations, plot fit samples, or rendered pages. Markdown and plain text are lossy interchange formats, not the canonical document model.

## Development Status

Scholar is pre-1.0. M18-M34 are implemented and manually accepted. The next planned milestone is M35, **Scholar API & Addon Framework**; it has not begun. The concise forward plan is M36 Import / Export & Interchange, M37 Authoring UX & Document Workflow, M38 V1 Product Hardening, M39 V1 Release Candidate, then Scholar 1.0. See the [Roadmap](docs/ROADMAP.md) and [Milestone History](docs/MILESTONE_HISTORY.md); plans remain subject to review.

## Contributing

There is no separate contribution policy yet. For proposed changes, include focused regression tests, run `test` and `build`, and preserve the core/client and semantic/derived-state boundaries described in the architecture document. Minecraft visual changes also need in-game review.

## License

The mod metadata currently declares `All Rights Reserved`; the repository does not include a general open-source license. Bundled Source Sans 3 and Noto Sans Math fonts have their own [OFL license texts](docs/licenses/fonts/).
