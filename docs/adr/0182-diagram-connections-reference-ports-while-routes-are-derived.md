# ADR 0182 - Diagram Connections Reference Ports While Routes Are Derived

## Status

Accepted

## Context

A scientific diagram connection has semantic meaning because of what it connects, not because of the exact screen polyline chosen to display it. Persisting renderer-generated paths would make ordinary element movement require rewriting presentation geometry and would couple the AST to a routing algorithm.

## Decision

A `DiagramConnection` stores source and target `DiagramEndpoint` references plus optional plain-string semantic label content. It does not store its rendered polyline.

The first visible route is derived deterministically during pure-Java layout using orthogonal segments and port-side exit direction. Obstacle avoidance, user-authored waypoints, curved paths, crossing bridges, and domain-specific net/joint semantics are deferred.

Connections are undirected in the first M17 slice.

## Alternatives Considered

- Persist routed polyline points in every connection.
- Draw a single direct diagonal segment regardless of port orientation.
- Implement obstacle-avoiding graph routing immediately.
- Make connections directed/arrows by default.

## Consequences

Moving an element automatically produces new connection geometry without changing semantic connectivity. The routing algorithm can be improved later without migrating the AST. Initial routes may cross elements or other connections, which is accepted for the foundation slice.
