# ADR 0191 - External Diagram Text Is Not Silently Inferred As DiagramBlock

## Status

Accepted

## Context

The plain-text fallback deliberately resembles a structural diagram description. Ordinary user text may contain similar labels, IDs, arrows, or bounds, and M17 has no public diagram interchange grammar.

## Decision

Scholar reconstructs DiagramBlock only from a matching process-local `DiagramClipboardPayload`. Plain external text is never silently parsed or inferred into a diagram. The existing exact-text sidecar rule invalidates the native payload when the OS clipboard text changes.

## Alternatives Considered

- Parse any matching diagram-summary text automatically.
- Infer diagrams from arrow/graph-like plain text.
- Introduce Mermaid, Graphviz, JSON, or Markdown import as part of clipboard paste.

## Consequences

Paste remains explicit and predictable. Future external diagram interchange can be designed as a separate user-invoked feature without freezing the M17 fallback text into an accidental file format.
