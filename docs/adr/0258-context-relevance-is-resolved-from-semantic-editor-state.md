# ADR 0258 - Context Relevance Is Resolved From Semantic Editor State

## Status

Accepted

## Context

Scholar has multiple editing modes: text, math, table cells, plots, diagrams, figures, and atomic blocks. Context menus need to show relevant actions without the UI reimplementing every editing rule.

## Decision

`EditorContextActionResolver` derives context entries from `EditorState` selection kind and selected semantic target. The widget only renders and executes entries.

## Alternatives Considered

- Let each screen branch directly over every possible target. This would tie semantic action relevance to Minecraft UI code.
- Show every registered action and rely on disabled states. This would make context menus noisy and hard to use.

## Consequences

The UI remains thin and action relevance can be tested with ordinary Java unit tests. Future selection kinds can extend the resolver without changing the rendering widget.
