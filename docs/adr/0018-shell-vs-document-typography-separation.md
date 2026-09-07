# ADR 0018 - Shell vs Document Typography Separation

## Status

Accepted

## Context

Scholar runs inside Minecraft. The surrounding UI should still feel native, but long-form scientific document reading has different typography needs than ordinary Minecraft interface labels.

## Decision

Scholar separates Minecraft shell typography from Scholar document-surface typography. The shell keeps normal Minecraft UI typography, while document content resolves presentation through Scholar's typography profile.

## Alternatives Considered

- Route all Minecraft UI text through Scholar typography.
- Keep document text permanently tied to the Minecraft default UI font.
- Store typography choices directly in the semantic document model.

## Consequences

Future document fonts can improve reading without making the whole game UI feel non-native. Developers must keep typography profile use scoped to document rendering surfaces.
