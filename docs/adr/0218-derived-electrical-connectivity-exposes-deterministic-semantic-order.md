# ADR 0218 — Derived Electrical Connectivity Exposes Deterministic Semantic Order

## Status
Accepted for M18G.

## Decision
`ElectricalNetResolver` remains a derived semantic connectivity pass, but the public immutable collections returned by `ElectricalNet` and `ElectricalConnectivity` preserve the resolver's deterministic insertion order.

`ElectricalNet.endpoints` is exposed as an unmodifiable `LinkedHashSet`, and `ElectricalConnectivity.netIndexByEndpoint` as an unmodifiable `LinkedHashMap`. The resolver's semantic traversal order therefore remains observable and reproducible instead of being discarded by unordered immutable-copy factories.

This does not make endpoint or net order authored document state. Connectivity equality and electrical meaning continue to come from explicit `DiagramConnection` endpoints and explicit junction internals, never from iteration order or wire geometry.

## Consequences
- Tests, diagnostics, debugging, and future deterministic serialization helpers can inspect derived connectivity in stable semantic order.
- No caller can mutate the returned endpoint set or endpoint-to-net map.
- Net meaning is unchanged; ordering is a reproducibility contract for the derived view only.
