package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramDefinition;
import dev.rgcb.scholar.diagram.DiagramEndpoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure-Java electrical topology resolver.
 *
 * <p>DiagramConnection edges electrically join their endpoints. Every port on
 * one ElectricalJunction is also internally equivalent. Mere visual wire
 * crossings are intentionally irrelevant because routed geometry is not an
 * input to this resolver.</p>
 */
public final class ElectricalNetResolver {
    public ElectricalConnectivity resolve(DiagramDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        var orderedEndpoints = new ArrayList<DiagramEndpoint>();
        var parents = new HashMap<DiagramEndpoint, DiagramEndpoint>();
        var ranks = new HashMap<DiagramEndpoint, Integer>();
        var junctionLabels = new HashMap<DiagramEndpoint, String>();

        for (var element : definition.elements()) {
            var endpoints = element.ports().stream()
                    .map(port -> new DiagramEndpoint(element.id(), port.id()))
                    .toList();
            for (var endpoint : endpoints) {
                if (!parents.containsKey(endpoint)) {
                    parents.put(endpoint, endpoint);
                    ranks.put(endpoint, 0);
                    orderedEndpoints.add(endpoint);
                }
            }
            if (element instanceof ElectricalJunction junction && !endpoints.isEmpty()) {
                var anchor = endpoints.get(0);
                for (var index = 1; index < endpoints.size(); index++) {
                    union(anchor, endpoints.get(index), parents, ranks);
                }
                if (!junction.netLabel().isBlank()) {
                    junctionLabels.put(anchor, junction.netLabel());
                }
            }
        }

        for (var connection : definition.connections()) {
            union(connection.source(), connection.target(), parents, ranks);
        }

        var grouped = new LinkedHashMap<DiagramEndpoint, LinkedHashSet<DiagramEndpoint>>();
        for (var endpoint : orderedEndpoints) {
            var root = find(endpoint, parents);
            grouped.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).add(endpoint);
        }

        // Preserve only electrically meaningful groups: a connected wire group,
        // or an explicit junction even if it is not wired yet. Unconnected
        // component terminals are not promoted to standalone nets.
        var connectionEndpoints = new LinkedHashSet<DiagramEndpoint>();
        definition.connections().forEach(connection -> {
            connectionEndpoints.add(connection.source());
            connectionEndpoints.add(connection.target());
        });
        var junctionEndpointIds = new LinkedHashSet<DiagramEndpoint>();
        definition.elements().stream()
                .filter(ElectricalJunction.class::isInstance)
                .map(ElectricalJunction.class::cast)
                .forEach(junction -> junction.ports().forEach(port ->
                        junctionEndpointIds.add(new DiagramEndpoint(junction.id(), port.id()))));

        var nets = new ArrayList<ElectricalNet>();
        var endpointToNet = new LinkedHashMap<DiagramEndpoint, Integer>();
        for (var group : grouped.values()) {
            var meaningful = group.stream().anyMatch(connectionEndpoints::contains)
                    || group.stream().anyMatch(junctionEndpointIds::contains);
            if (!meaningful) {
                continue;
            }
            var label = resolveLabel(group, definition);
            var net = new ElectricalNet(nets.size(), label, group);
            nets.add(net);
            for (var endpoint : group) {
                endpointToNet.put(endpoint, net.index());
            }
        }
        return new ElectricalConnectivity(nets, endpointToNet);
    }

    private static Optional<String> resolveLabel(
            LinkedHashSet<DiagramEndpoint> endpoints,
            DiagramDefinition definition
    ) {
        String resolved = null;
        for (var element : definition.elements()) {
            if (!(element instanceof ElectricalJunction junction) || junction.netLabel().isBlank()) {
                continue;
            }
            var belongs = junction.ports().stream()
                    .map(port -> new DiagramEndpoint(junction.id(), port.id()))
                    .anyMatch(endpoints::contains);
            if (!belongs) {
                continue;
            }
            if (resolved == null) {
                resolved = junction.netLabel();
            } else if (!resolved.equals(junction.netLabel())) {
                throw new IllegalArgumentException(
                        "One electrical net cannot contain conflicting junction labels: "
                                + resolved + " vs " + junction.netLabel());
            }
        }
        return Optional.ofNullable(resolved);
    }

    private static DiagramEndpoint find(
            DiagramEndpoint endpoint,
            Map<DiagramEndpoint, DiagramEndpoint> parents
    ) {
        var parent = parents.get(endpoint);
        if (parent == null) {
            throw new IllegalArgumentException("Unknown electrical endpoint: " + endpoint);
        }
        if (parent.equals(endpoint)) {
            return endpoint;
        }
        var root = find(parent, parents);
        parents.put(endpoint, root);
        return root;
    }

    private static void union(
            DiagramEndpoint first,
            DiagramEndpoint second,
            Map<DiagramEndpoint, DiagramEndpoint> parents,
            Map<DiagramEndpoint, Integer> ranks
    ) {
        var firstRoot = find(first, parents);
        var secondRoot = find(second, parents);
        if (firstRoot.equals(secondRoot)) {
            return;
        }
        var firstRank = ranks.get(firstRoot);
        var secondRank = ranks.get(secondRoot);
        if (firstRank < secondRank) {
            parents.put(firstRoot, secondRoot);
        } else if (firstRank > secondRank) {
            parents.put(secondRoot, firstRoot);
        } else {
            parents.put(secondRoot, firstRoot);
            ranks.put(firstRoot, firstRank + 1);
        }
    }
}
