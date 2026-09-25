# Scholar 1.0 release notes (draft)

This is a draft for the upcoming 1.0.0 release, **not** an announcement or final artifact. `1.0.0-rc.1` is under clean-install qualification.

Scholar is a scientific document editor inside Minecraft Java. Create readable, structured local documents with prose, equations, quantities, datasets, tables, plots, analyses, figures, diagrams and cross-references. The document model is semantic; page layout, labels and calculations are derived. Scholar is independent of any future STEM Lab mod.

## Requirements and installation

- Minecraft Java Edition 1.21.1, NeoForge 21.1.249 or newer within 21.1.x, Java 21.
- Place the exact Scholar RC JAR in the instance's `mods` directory. The reference addon is optional and should not be installed for the first clean-boot test.
- Enter a world and run `/scholar`. Home creates blank/template documents and opens saved ones. Use File > Save; close protection offers Save, Discard or Cancel.

## Scientific workflow

Write headings and prose; insert equations, tables, plots, figures, diagrams and references through the ribbon. Create datasets and derive table/plot views and statistical analyses. Variables and computed results use structured quantity/unit semantics. Page settings support one or two columns and export-ready pagination.

## Files and interchange

The durable local format is `.scholar.json` (current writes: schema V2; V1 remains readable). CSV imports/exports datasets, Markdown exports readable text, and PDF exports paginated output. CSV import is limited to 16 MiB in V1; Markdown is intentionally lossy for Scholar-only structures and is not a backup format.

Existing supported Scholar documents should remain readable without migration. Back up the `scholar` data directory before upgrading; verify Home, open, and save on the RC before replacing a working installation permanently. M39's installed-artifact upgrade test remains pending.

## Addons and limits

The documented client-side API under `dev.rgcb.scholar.api` lets addons create/edit supported Scholar content; see [M35 API](M35_SCHOLAR_API.md). V1 does not offer arbitrary addon-defined blocks, a custom unit-registry plugin system, server-side document editing, or a programming notebook. Full layout and PDF preparation are synchronous for very large documents. The project license remains an owner decision before public 1.0.0 distribution; bundled fonts have separate OFL terms.

The final release version, checksum, tag and public release notes must be updated only after M39 manual acceptance.
