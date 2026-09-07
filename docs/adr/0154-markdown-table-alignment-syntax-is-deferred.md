# ADR 154 - Markdown Table Alignment Syntax Is Deferred

## Status

Accepted

## Context

GitHub-style Markdown supports separator alignment markers, but Scholar's table AST currently has no column alignment model.

## Decision

M15F accepts only separator cells made of three or more hyphens. Alignment syntax such as `:---`, `---:`, and `:---:` is rejected instead of parsed and discarded.

## Alternatives Considered

- Parse alignment markers and ignore them.
- Add a table alignment model now.
- Preserve alignment as Markdown-specific metadata.

## Consequences

Markdown interchange does not silently lose supported-looking author intent. Alignment can be added later when the AST and renderer have an explicit semantic place for it.
