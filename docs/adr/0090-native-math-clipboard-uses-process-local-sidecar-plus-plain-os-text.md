# 0090 - Native Math Clipboard Uses Process-Local Sidecar Plus Plain OS Text

## Status

Accepted

## Context

Minecraft and GLFW expose string clipboard APIs, but Scholar needs to preserve math AST structure when copying and pasting inside the same client process.

## Decision

Math Copy and Cut write a readable plain-text fallback to the OS clipboard and store a structured Scholar payload in a process-local sidecar only after the OS clipboard write succeeds.

## Alternatives Considered

- Store only plain text on the OS clipboard.
- Use custom native clipboard flavors.
- Encode hidden metadata in visible clipboard text.

## Consequences

External applications receive readable text, while same-process Scholar paste can preserve AST structure. Structured clipboard state is lost after restart and is not cross-process in v0.1.
