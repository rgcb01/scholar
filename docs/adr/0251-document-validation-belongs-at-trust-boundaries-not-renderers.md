# ADR 0251 - Document Validation Belongs At Trust Boundaries Not Renderers

## Status

Accepted

## Context

Rendering, layout, editing, import, clipboard, and future persistence all consume the same semantic document model. Validation should be reusable across these flows without making every consumer reimplement structural checks.

## Decision

Document validation is centralized in `dev.rgcb.scholar.validation` and is intended for trust boundaries such as import/load/save and explicit diagnostics. Renderers and layout engines should not become the authoritative validators.

## Alternatives Considered

- Let each subsystem validate what it needs. This duplicates rules and produces inconsistent behavior.
- Validate only in constructors. Constructors remain important but cannot report aggregate degraded-state diagnostics.

## Consequences

Core validation remains Minecraft-independent and can evolve as the document model grows. Consumers may still keep local precondition checks, but canonical document diagnostics come from the validator.
