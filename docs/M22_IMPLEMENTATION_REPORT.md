# M22 Implementation Report - Document Structure & Navigation

## Status

Implemented with automated validation complete. Manual Minecraft QA remains pending.

## Implemented

- Derived `DocumentStructure` from the existing flat `Document -> List<BlockNode>` model.
- Added `SectionEntry`, `SectionNumber`, and `DocumentStructureResolver`.
- Added deterministic hierarchical section numbering for `Heading` blocks.
- Chose explicit zero placeholders for skipped levels, for example `1.0.1`.
- Upgraded section cross-reference labels to hierarchical labels through the shared structure resolver.
- Rendered heading section numbers as display-only layout prefixes.
- Added semantic `TableOfContentsBlock`; entries are derived during layout/plain-text serialization.
- Added TOC hit regions for navigation by stable heading ID.
- Added an editor outline panel derived from the same structure resolver.
- Added navigation to heading IDs without undo history mutation.
- Added native clipboard payload support for heading and TOC block copy/paste.
- Remapped duplicate heading IDs on structured paste.
- Kept Markdown headings clean and unnumbered; TOC exports as a minimal readable placeholder.
- Added development fixtures for TOC, hierarchy, skipped levels, and section references.

## Validation

- `.\gradlew.bat test`
- `.\gradlew.bat build`

## Out Of Scope

- Persistent `Section` AST.
- Page numbers.
- Pagination.
- Auto-generated heading IDs.
- Citation or bibliography systems.
- Full Markdown TOC syntax.
- M23 work.
