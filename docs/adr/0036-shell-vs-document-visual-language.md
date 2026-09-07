# 0036 Shell vs Document Visual Language

Status: Accepted

## Context

Scholar has two visual jobs: application controls must belong inside Minecraft, while scientific content must stay readable for learning.

## Decision

Keep the application shell Minecraft-native and keep the document surface readability-first. The shell uses Minecraft UI conventions and font; the document continues using Scholar typography such as Source Sans 3 and Noto Sans Math.

## Alternatives Considered

- Render all text in Minecraft's pixel font: rejected because it harms scientific document readability.
- Make the entire editor resemble a desktop document editor: rejected because it ignores the Minecraft host.
- Reopen typography selection for shell work: rejected because document typography is already settled for v0.1.

## Consequences

Shell work can evolve independently from document typography. GUI scale affects shell chrome, while document zoom and typography remain separate future concerns.
