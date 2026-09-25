# Scholar 1.0.0 release notes

Release preparation only. Scholar 1.0.0 has not been tagged or published. The project license and NeoForge author display name require owner decisions before the final artifact can be built and verified.

Scholar is a scientific document editor inside Minecraft Java. Create readable, structured local documents with prose, headings, equations, quantities, datasets, tables, plots, analyses, figures, diagrams, cross-references and a derived table of contents. Page layout, reference labels and calculations derive from a semantic document model. Scholar is independent of any future STEM Lab mod.

## Requirements and installation

- Minecraft Java Edition 1.21.1, NeoForge in the declared range `[21.1.249,21.2)`, and Java 21.
- Once released, place `scholar-1.0.0.jar` in the Minecraft instance's `mods/` directory. The separate reference addon is optional and is not part of the Scholar JAR.
- Enter a world and run `/scholar`. Home creates blank/template documents and opens saved ones. Use File > Save; close protection offers Save, Discard or Cancel.

## Scientific workflow

Write headings and prose; insert equations, tables, plots, figures, electrical and mechanical diagrams and references through the ribbon. Create datasets and derive table/plot views, statistics and polynomial fits. Variables and computed results use structured quantity/unit semantics. Page settings support one or two columns and export-ready pagination. Semantic copy/cut/paste and undo/redo support editing.

## Files and interchange

The durable local format is `.scholar.json` (current writes: schema V2; supported V1 files remain readable). The application-version change does not change the document schema or rewrite existing documents. CSV imports/exports datasets, Markdown exports readable text, and PDF exports paginated output. CSV import is limited to 16 MiB; Markdown is intentionally lossy for Scholar-only structures and is not a backup format.

Back up the `scholar` data directory before upgrading. The accepted RC installation test covered reopening supported older documents without migration.

## Addons and limits

The documented client-side API under `dev.rgcb.scholar.api` lets addons create/edit supported Scholar content; see [M35 API](M35_SCHOLAR_API.md). V1 does not offer arbitrary addon-defined blocks, a custom unit-registry plugin system, server-side document editing, or a programming notebook. Full layout and PDF preparation are synchronous for very large documents. Documents are stored locally by the Minecraft client.

Bundled fonts have separate SIL OFL notices. The Scholar project license remains an owner decision; do not infer it from the font or PDFBox licenses. The final JAR checksum and release tag must be recorded only after final artifact qualification. Do not use the accepted RC checksum for the 1.0.0 JAR.
