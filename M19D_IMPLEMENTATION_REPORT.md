# M19D Implementation Report — Mechanical Symbols

M19D adds six first-class semantic mechanical symbols: SHAFT, GEAR, BEARING, SPRING, PISTON, and BOLT. Each is authored as a `MechanicalSymbol` element; visible strokes are derived programmatically and are not persisted as pixel geometry.

Integration includes authoring actions, scrollable Diagram menu entries, selection/hit testing, drag through the generic diagram element path, deletion/history, layout, rendering, native clipboard compatibility, plain-text fallback, and a `Mechanical Symbols` development fixture.

Validation in this environment: Java 21 core compilation passed; all client Java sources compile against the existing narrow Minecraft/NeoForge stubs; 30 focused mechanical/diagram/electrical tests pass, including 4 new M19D symbol tests.
