package dev.rgcb.scholar.compute;

import dev.rgcb.scholar.document.ComputedResult;
import dev.rgcb.scholar.document.Document;
import dev.rgcb.scholar.document.VariableDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Editor-owned derived cache; semantic document values remain the sole authority. */
public final class ComputationEngine {
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final IdentityHashMap<ComputedResult, Entry> cache = new IdentityHashMap<>();
    private Document document;
    private ComputationSnapshot snapshot = new ComputationSnapshot(Map.of());
    private long evaluationCount;

    public ComputationSnapshot update(Document next) {
        if (document == next) return snapshot;
        var variables = new HashMap<String, VariableDefinition>();
        var names = new HashMap<String, Long>();
        next.blocks().stream().filter(VariableDefinition.class::isInstance).map(VariableDefinition.class::cast)
                .forEach(variable -> {
                    variables.put(variable.id(), variable);
                    names.merge(variable.name(), 1L, Long::sum);
                });
        var results = new HashMap<Integer, ComputationResult>();
        var survivors = new IdentityHashMap<ComputedResult, Entry>();
        for (var index = 0; index < next.blocks().size(); index++) {
            if (!(next.blocks().get(index) instanceof ComputedResult computed)) continue;
            var signature = new ArrayList<Object>();
            for (var dependency : computed.variableDependencies()) {
                var variable = variables.get(dependency.variableId());
                signature.add(variable);
            }
            if (hasUnresolvedName(computed.expression())) {
                // Unbound names can change diagnostic state as definitions change.
                signature.add(names);
            }
            var existing = cache.get(computed);
            var value = existing != null && existing.signature().equals(signature)
                    ? existing.result() : evaluator.evaluate(computed.expression(), next);
            if (existing == null || !existing.signature().equals(signature)) evaluationCount++;
            var entry = new Entry(List.copyOf(signature), value);
            survivors.put(computed, entry);
            results.put(index, value);
        }
        cache.clear();
        cache.putAll(survivors);
        document = next;
        snapshot = new ComputationSnapshot(results);
        return snapshot;
    }

    public long evaluationCount() { return evaluationCount; }

    private static boolean hasUnresolvedName(Expression expression) {
        if (expression instanceof Expression.UnresolvedName) return true;
        if (expression instanceof Expression.Unary unary) return hasUnresolvedName(unary.operand());
        if (expression instanceof Expression.Binary binary) {
            return hasUnresolvedName(binary.left()) || hasUnresolvedName(binary.right());
        }
        if (expression instanceof Expression.Power power) return hasUnresolvedName(power.base());
        if (expression instanceof Expression.Group group) return hasUnresolvedName(group.inner());
        return false;
    }

    private record Entry(List<Object> signature, ComputationResult result) {}
}
