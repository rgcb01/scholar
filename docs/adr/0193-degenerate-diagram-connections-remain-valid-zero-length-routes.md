# ADR 0193: Degenerate diagram connections remain valid zero-length routes

## Status
Accepted

## Context

The semantic model intentionally permits overlapping elements, repeated port placements, and connections between distinct endpoints on the same element. Two distinct valid semantic endpoints can therefore resolve to the exact same laid-out coordinate. The orthogonal router previously removed duplicate consecutive points, which could leave a one-point path and violate `LaidOutDiagramConnection`'s minimum two-point contract.

## Decision

When routing collapses completely, `DiagramConnectionRouter` emits two equal points representing one deterministic zero-length segment. The semantic connection is preserved; layout does not invent displacement or reject the authored structure.

## Consequences

Degenerate but structurally valid diagrams no longer crash layout. Existing render and hit-test code can consume the normal two-point path contract. Obstacle avoidance, visual separation of coincident connections, and authoring warnings remain deferred.
