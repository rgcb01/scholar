# ADR 0238: Outline View Is Derived UI State

## Status

Accepted

## Context

The editor needs an outline/navigation view, but outline expansion, scrolling, and clicks are presentation concerns rather than document content.

## Decision

The outline panel is a Minecraft client UI surface derived from the current `DocumentStructure`. It is not stored in the document AST and navigation does not create undo history.

## Alternatives Considered

- Store an outline block or metadata in the document. This would mix UI state into semantic content.
- Build a separate outline model in the client. This risks diverging from references and TOC numbering.

## Consequences

The outline remains consistent with TOC and section references. It can be redesigned later without migrating document data.
