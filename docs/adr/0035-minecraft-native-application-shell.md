# 0035 Minecraft-Native Application Shell

Status: Accepted

## Context

Scholar runs inside Minecraft, but it is a serious scientific editor. The surrounding application shell should feel native to the game instead of imitating a desktop or web application.

## Decision

Build shell chrome with Minecraft-style visual language: compact vanilla-font labels, pixel-aligned fills, simple panel borders, hover/pressed/disabled states, and restrained game-native spacing. Do not create a custom sprite atlas for the first shell slice.

## Alternatives Considered

- Imitate native desktop menus: rejected because it would feel pasted over Minecraft.
- Use modern web/SaaS styling: rejected because it conflicts with Minecraft's UI language.
- Create a custom texture atlas immediately: deferred until the shell needs richer iconography.

## Consequences

Scholar's controls feel like they belong in Minecraft while remaining subdued enough for focused document work. More polished textures can be added later without changing the shell architecture.
