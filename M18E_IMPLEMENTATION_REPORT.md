# M18E Implementation Report — Nets + Junctions + Electrical Connectivity

## Scope

Implemented M18E on top of the user-validated M18D project. This slice adds explicit electrical junction authoring and a pure-Java semantic net resolver while preserving the M17/M18 rule that visual wire geometry never defines connectivity.

## New semantic model

- `ElectricalJunction implements DiagramElement`
  - stable element id
  - logical bounds
  - optional net label
  - four directional ports: `left`, `right`, `top`, `bottom`
- `ElectricalNet`
- `ElectricalConnectivity`
- `ElectricalNetResolver`

The four ports on one junction are routing anchors only; `ElectricalNetResolver` electrically unions them into one node.

## Connectivity policy

Electrical topology is derived only from:

1. authored `DiagramConnection` endpoint edges; and
2. internal equivalence of ports owned by an explicit `ElectricalJunction`.

Rendered paths, elbows, wire crossings, pixel contact, and overlapping strokes are not inputs. Therefore two visually crossing wires remain independent unless an explicit semantic junction joins them.

Unconnected component terminals are not promoted to standalone nets. Explicit junctions may exist before wires are attached and still resolve as a semantic net node.

## Net labels

A junction can carry a label such as `VOUT`. The label names the already-connected derived net. Equal labels on disconnected nets do not silently merge topology. If two different non-empty junction labels occur inside the same connected net, the resolver rejects that conflicting connectivity snapshot.

## Authoring

Added Diagram menu actions:

- `Add Junction`
- `Delete Junction`

Junctions reuse generic M17 dragging, selection, endpoint validation, connection drafts, history, and whole-diagram clipboard behavior.

Workflow:

- add junction;
- drag it as a normal diagram element;
- click the center/dot to select the junction;
- press Enter to edit its net label through the existing diagram-label popup;
- select a side port to start/finish wire connections;
- delete the junction and all incident wire segments in one history transaction.

The hit-testing policy deliberately distinguishes the center dot from the four nearby port hit regions so the junction remains both movable and connectable.

## Layout / rendering

- Added `LaidOutElectricalJunction`.
- Junction dots render after wires, so a filled dot unambiguously communicates a splice.
- Junction port endpoints use zero extra exit reservation, avoiding an artificial lead stub at the splice.
- Component electrical terminals retain the longer M18 straight-lead reservation.
- Optional net labels are laid out beside the junction in pure Java.

## Development fixture

Added `Junction + Net` to the development editor. It contains a labeled `VOUT` junction feeding two branches so manual QA can verify one explicit three-way electrical net.

## Clipboard

The existing whole-`DiagramBlock` native clipboard already preserves the new junction model losslessly. `DiagramPlainTextSerializer` now emits junction bounds, ports, and optional net label in the readable external fallback.

## Tests

Added 11 ordinary JUnit tests across:

- `ElectricalNetResolverTest`
- `ElectricalJunctionAuthoringTest`
- `ElectricalJunctionLayoutTest`

Expected authoritative Gradle baseline: **885 test executions** (883 ordinary `@Test` methods plus the existing parameterized executions), assuming the prior M18D baseline remains unchanged.

Local validation completed in this environment:

- pure-Java production compilation with Java 21: PASS
- `DevelopmentDocument` compilation against compiled core: PASS
- new test sources type-check against JUnit-compatible stubs: PASS
- executable net-resolution smoke test: PASS
- executable junction layout/hit-testing smoke test: PASS
- core boundary scan for Minecraft/NeoForge/Mojang imports: clean

Full Gradle execution could not run because the sandbox cannot resolve `services.gradle.org` to download Gradle 9.2.1.

## ADRs

Added:

- ADR 0210 — Electrical Junctions Are Explicit Semantic Diagram Elements
- ADR 0211 — Electrical Nets Are Derived From Semantic Connectivity, Not Wire Geometry
- ADR 0212 — Junction Directional Ports Are Internally One Electrical Node
- ADR 0213 — Net Labels Annotate Explicit Connected Nets Without Merging By Name

Updated `ELECTRICAL_DIAGRAM_SPEC.md` and `ROADMAP.md`. M18D is recorded as manually accepted; M18E is implemented/manual-QA pending. M18E.5 workspace scaling remains next after M18E validation.

## Deliberately deferred

M18E does not add:

- geometry-based automatic junction detection;
- automatic net merging by equal label;
- buses;
- electrical-rule checking;
- simulation/evaluation;
- obstacle avoidance;
- zoom/pan/canvas resizing (M18E.5);
- UX palette/drag-to-place/snapping polish.
