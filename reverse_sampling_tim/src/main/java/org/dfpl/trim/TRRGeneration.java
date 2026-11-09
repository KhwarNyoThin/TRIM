package org.dfpl.trim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.dfpl.element.Direction;
import org.dfpl.element.EdgeEvent;
import org.dfpl.element.Graph;
import org.dfpl.element.Vertex;

public final class TRRGeneration {
    private TRRGeneration() {}

    /** Samples one TRR set with fresh event states using the supplied RNG. */
    public static Set<Vertex> generateTRR(Graph graph, Random random) {
        return generateTRR(prepare(graph), random);
    }

    // Immutable index snapshot; rebuild after any graph/event mutation
    public static final class PreparedGraph {
        private final Graph graph;
        private final List<Vertex> vertices;
        private final Map<Vertex, List<EdgeEvent>> incoming;

        private PreparedGraph(Graph graph) {
            this.graph = Objects.requireNonNull(graph, "graph");
            List<Vertex> sortedVertices = new ArrayList<>(graph.getVertices());
            sortedVertices.sort(Comparator.comparing(Vertex::getId));
            vertices = List.copyOf(sortedVertices);
            Map<Vertex, List<EdgeEvent>> index = new HashMap<>();
            for (Vertex vertex : vertices) {
                List<EdgeEvent> events = new ArrayList<>(graph.getInEdgeEvents(vertex));
                events.sort(Comparator.naturalOrder());
                index.put(vertex, List.copyOf(events));
            }
            incoming = Map.copyOf(index);
        }

        void requireGraph(Graph expected) {
            if (graph != expected) {
                throw new IllegalArgumentException("Prepared index belongs to a different graph");
            }
        }
    }

    // Prepare once and reuse across collections while the graph remains unchanged
    public static PreparedGraph prepare(Graph graph) {
        return new PreparedGraph(graph);
    }

    /** Generates a new TRR sample with fresh traversal state and event outcomes,
    *  reusing the prepared vertex and incoming-event lists. */
    public static Set<Vertex> generateTRR(PreparedGraph prepared, Random random) {
        Objects.requireNonNull(prepared, "prepared");
        Objects.requireNonNull(random, "random");
        Graph graph = prepared.graph;
        List<Vertex> vertices = prepared.vertices;
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("Cannot sample a target from an empty graph");
        }
        Vertex target = vertices.get(random.nextInt(vertices.size()));
        Set<Vertex> result = new LinkedHashSet<>();
        result.add(target);
        // Target has +infinity permanently. Missing non-target bounds mean -infinity.
        // Keeping infinity separate allows both Long.MIN_VALUE and Long.MAX_VALUE.
        Map<Vertex, Long> bounds = new HashMap<>();
        Map<EdgeEvent, Boolean> cache = new HashMap<>();
        Queue<Vertex> queue = new ArrayDeque<>();
        queue.add(target);

        while (!queue.isEmpty()) {
            Vertex x = queue.poll();
            List<EdgeEvent> incoming = prepared.incoming.get(x);
            // The target has conceptual +infinity, including Long.MAX_VALUE events.
            int end = x.equals(target) ? incoming.size()
                    : eligiblePrefixEnd(incoming, bounds.get(x));
            for (int i = 0; i < end; i++) {
                EdgeEvent event = incoming.get(i);
                long t = event.getTime().getTime();
                boolean live = cache.computeIfAbsent(event, e -> {
                    double p = graph.getPropagationProbability(e);
                    return random.nextDouble() < p;
                });
                if (live) {
                    Vertex u = event.getVertex(Direction.OUT);
                    result.add(u);
                    // A finite timestamp can never improve the target's +infinity.
                    if (!u.equals(target) && (!bounds.containsKey(u) || t > bounds.get(u))) {
                        bounds.put(u, t);
                        queue.add(u);
                    }
                }
            }
        }
        return result;
    }

    // First index with timestamp >= bound; equal timestamps are excluded
    private static int eligiblePrefixEnd(List<EdgeEvent> incoming, long bound) {
        int low = 0;
        int high = incoming.size();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (incoming.get(mid).getTime().getTime() < bound) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

}
