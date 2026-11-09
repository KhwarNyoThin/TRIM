package org.dfpl.trim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.dfpl.element.Direction;
import org.dfpl.element.EdgeEvent;
import org.dfpl.element.Graph;
import org.dfpl.element.Vertex;

// Independent forward TIC simulation. The graph must remain unchanged during a run
public final class TICSimulator {
    private TICSimulator() {}

    // Returns the number of distinct active vertices in one diffusion realization
    public static int simulate(Graph graph, Set<Vertex> seeds, Random random) {
        Set<Vertex> seedSnapshot = validate(graph, seeds, random);
        return simulatePrepared(graph, seedSnapshot, prepareEvents(graph), random);
    }

    /** Estimates influence using exactly simulations fresh diffusion realizations. */
    public static double estimateInfluence(Graph graph, Set<Vertex> seeds,
                                           int simulations, Random random) {
        Set<Vertex> seedSnapshot = validate(graph, seeds, random);
        if (simulations <= 0) {
            throw new IllegalArgumentException("simulations must be positive");
        }
        List<EdgeEvent> events = prepareEvents(graph);
        long total = 0;
        for (int i = 0; i < simulations; i++) {
            total += simulatePrepared(graph, seedSnapshot, events, random);
        }
        return (double) total / simulations;
    }

    private static Set<Vertex> validate(Graph graph, Set<Vertex> seeds, Random random) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(seeds, "seeds");
        Objects.requireNonNull(random, "random");
        Set<Vertex> seedSnapshot = new HashSet<>(seeds);
        Set<Vertex> vertices = new HashSet<>(graph.getVertices());
        for (Vertex seed : seedSnapshot) {
            if (seed == null || !vertices.contains(seed)) {
                throw new IllegalArgumentException("All seeds must belong to the graph: " + seed);
            }
        }
        return seedSnapshot;
    }

    private static List<EdgeEvent> prepareEvents(Graph graph) {
        List<EdgeEvent> events = new ArrayList<>(graph.getEdgeEvents());
        // EdgeEvent orders by timestamp, then edge ID for reproducible ties.
        events.sort(Comparator.naturalOrder());
        return events;
    }

    private static int simulatePrepared(Graph graph, Set<Vertex> seeds,
                                         List<EdgeEvent> events, Random random) {
        // Seeds precede every timestamp, including Long.MIN_VALUE; no sentinel is used.
        // Only non-seed activation times are stored here.
        Map<Vertex, Long> activationTimes = new HashMap<>();
        for (EdgeEvent event : events) {
            Vertex source = event.getVertex(Direction.OUT);
            Vertex target = event.getVertex(Direction.IN);
            long time = event.getTime().getTime();
            if (seeds.contains(target) || activationTimes.containsKey(target)) {
                continue;
            }
            if (!seeds.contains(source)) {
                Long activationTime = activationTimes.get(source);
                if (activationTime == null || activationTime >= time) {
                    continue;
                }
            }
            double probability = graph.getPropagationProbability(event);
            if (random.nextDouble() < probability) {
                activationTimes.put(target, time);
            }
        }
        return seeds.size() + activationTimes.size();
    }
}
