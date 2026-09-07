package dev.rgcb.scholar.electrical;

import dev.rgcb.scholar.diagram.DiagramEndpoint;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable derived connectivity snapshot for one diagram definition. */
public record ElectricalConnectivity(
        List<ElectricalNet> nets,
        Map<DiagramEndpoint, Integer> netIndexByEndpoint
) {
    public ElectricalConnectivity {
        nets = List.copyOf(Objects.requireNonNull(nets, "nets"));
        netIndexByEndpoint = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(netIndexByEndpoint, "netIndexByEndpoint")));
    }

    public Optional<ElectricalNet> netFor(DiagramEndpoint endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");
        var index = netIndexByEndpoint.get(endpoint);
        return index == null ? Optional.empty() : Optional.of(nets.get(index));
    }

    public boolean electricallyConnected(DiagramEndpoint first, DiagramEndpoint second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        var firstNet = netIndexByEndpoint.get(first);
        var secondNet = netIndexByEndpoint.get(second);
        return firstNet != null && firstNet.equals(secondNet);
    }
}
