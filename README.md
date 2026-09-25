# Scholar 1.0.0

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
- CSV dataset import/export, readable Markdown export, and paginated PDF export.

## Installation

Scholar `1.0.0` targets Minecraft Java Edition `1.21.1`, NeoForge `[21.1.249,21.2)`, and Java `21`. Install a compatible NeoForge client, place `scholar-1.0.0.jar` in that instance's `mods/` directory, launch Minecraft, enter a world, and run `/scholar`. The reference addon is optional and is not included in the Scholar JAR. The final artifact remains subject to release verification and owner approval.

To build from source on Windows, run `.\gradlew.bat clean build`; on Unix-like systems run `./gradlew clean build`. The Gradle `9.2.1` wrapper uses a Java 21 toolchain and produces the mod JAR in `build/libs/`. `runClient` is for development, not installation proof.

## Using Scholar

Enter a Minecraft world and run `/scholar`. Home lets you create a blank or template document, open an existing one, and manage saved documents; right-click a document card for Open, Rename, or Delete. Edit in the document workspace and use File > Save; closing a modified document prompts you to save, discard, or cancel. File also provides CSV import and Markdown/PDF export, while Data provides dataset CSV export. The M34 Readability Sample is an optional production template, not an automatically created file.

## Architecture

The semantic `Document` and its resources are canonical. Validation and editor transactions preserve invariants; layout derives pages, columns, geometry and labels; the Minecraft client renders that layout. Transfer and versioned persistence are separate boundaries, neither of which serializes transient view state. See [Architecture](docs/ARCHITECTURE.md) for the current layer map and ownership rules.

M35 adds a deliberately small client-side addon API for documents, datasets, measurements, units, variables, analyses, and dataset-backed visuals. Only `dev.rgcb.scholar.api` and its documented subpackages are supported integration contracts; see [Scholar Addon API](docs/M35_SCHOLAR_API.md) and the [addon linking exception](LICENSE-EXCEPTION).

## Developing Scholar Addons

Depend on the Scholar mod artifact and call `ScholarApi.get()` on the Minecraft client thread. Author normal Scholar content through `ScholarDocument.edit`, which validates and commits one history transaction. `examples/reference-addon` is a separate example mod; run it with `.\gradlew.bat runReferenceClient` and invoke `/scholar_reference_demo` in a world. The accepted M35 API does not offer arbitrary custom blocks or server-side document editing.

## File Format

Scholar saves local `.scholar.json` files using its versioned semantic JSON codec. Current writes use V2; supported V1 files load with deterministic defaults. The file stores authored content and stable semantic IDs, not selection, undo history, zoom, cached computations, plot fit samples, or rendered pages. Markdown and plain text are lossy interchange formats, not the canonical document model.

## Release Status and Limitations

M18-M39 are manually accepted. The 1.0.0 release is being prepared; it has not been tagged or published. See the [release notes](docs/RELEASE_1.0.md) and [changelog](CHANGELOG.md). Native `.scholar.json` is the durable document format; Markdown and plain text are lossy interchange, and CSV import has a 16 MiB limit. Addons can author supported Scholar content through the V1 API but cannot define arbitrary new document blocks. Documents are local to the Minecraft client. See the [Roadmap](docs/ROADMAP.md) for milestone status.

## Contributing

There is no separate contribution policy yet. For proposed changes, include focused regression tests, run `test` and `build`, and preserve the core/client and semantic/derived-state boundaries described in the architecture document. Minecraft visual changes also need in-game review.

## License

Scholar core is licensed under [GNU GPL-3.0-or-later](LICENSE) (`SPDX-License-Identifier: GPL-3.0-or-later`), with a [narrow linking exception](LICENSE-EXCEPTION) for independent addons using the documented public API. The separate [reference addon](examples/reference-addon/) is CC0-1.0; this does not relicense Scholar core. Bundled Source Sans 3 and Noto Sans Math fonts retain their own [OFL license texts](docs/licenses/fonts/), also included in the mod JAR. Bundled PDFBox components carry their own Apache license and notices inside their nested JARs. Author: Rómulo Colorado (rgcb0). The exception's final wording requires owner review before publication.
