# ADR 0187 - Incomplete Diagram Connections Live Only In Editor Interaction State

## Status

Accepted

## Context

Creating a connection requires choosing two semantic endpoints over multiple user interactions. Persisting a half-authored connection would violate `DiagramDefinition` endpoint invariants and would force the AST to represent temporary UI state.

## Decision

Connection authoring is explicit and two-stage. The editor first arms one selected `DiagramPortTarget` as an ephemeral source endpoint. The user then selects a distinct target port and explicitly finishes the connection.

The pending source lives only in `EditorSession` interaction state. No `DiagramConnection` is added to the document and no history entry is created until both endpoints are known. Escape/cancel, leaving diagram editing, starting a node drag, Undo/Redo, or structural replacement clears the pending source.

The source port receives a temporary visual outline in the Minecraft editor. Incomplete connections are never routed or rendered as semantic document content.

## Alternatives Considered

- Insert a connection with a missing target into `DiagramDefinition`.
- Infer connections automatically when nodes or ports become spatially close.
- Start and finish a connection in one drag gesture in the first structural slice.
- Store the pending endpoint inside `DiagramBlock`.

## Consequences

The semantic AST remains valid at every history snapshot, connection authoring is deterministic and cancelable, and later M18/M19 tools can replace the interaction affordance without changing the underlying connection model.
