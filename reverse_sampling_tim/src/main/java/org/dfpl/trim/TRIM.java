package org.dfpl.trim;

import org.dfpl.element.Graph;
import org.dfpl.element.Vertex;

import java.util.*;
import java.util.function.BiConsumer;

public final class TRIM {
    private TRIM() {}

    /**
     * Generates exactly theta TRR sets, then selects k vertex seeds.
     * All samples are stored in memory. The conservative ArrayList capacity limit
     * is Integer.MAX_VALUE - 8; available heap may impose a much smaller limit.
     * Requests above this capacity are rejected before any sampling occurs.
     * generation and selection durations in nanoseconds after computation.
     */
    public static Set<Vertex> runTRIM(Graph graph, int k, long theta, Random random,
                                     BiConsumer<Long, Long> runTRIMeConsumer,
                                     TRRGeneration.PreparedGraph prepared) {
        Objects.requireNonNull(prepared, "prepared").requireGraph(graph);
        Objects.requireNonNull(runTRIMeConsumer, "runTRIMeConsumer");
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(random, "random");
        if (k < 0 || k > graph.getVertices().size()) {
            throw new IllegalArgumentException("k must be between 0 and the number of vertices");
        }
        if (theta < 0) {
            throw new IllegalArgumentException("theta must be nonnegative");
        }
        if (theta > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("theta=" + theta
                    + " exceeds the current List-based TRR storage limit (Integer.MAX_VALUE - 8). "
                    + "All TRR sets must fit in memory; even smaller counts may exceed available heap.");
        }
        if (theta > 0 && graph.getVertices().isEmpty()) {
            throw new IllegalArgumentException("Cannot sample targets from an empty graph");
        }
        long generationStart = System.nanoTime();
        List<Set<Vertex>> trrSets = new ArrayList<>();
        for (long i = 0; i < theta; i++) {
            trrSets.add(TRRGeneration.generateTRR(prepared, random));
        }
        long generationrunTRIMe = System.nanoTime() - generationStart;
        long selectionStart = System.nanoTime();
        Set<Vertex> seeds = SeedSelection.selectSeeds(graph.getVertices(), trrSets, k);
        long selectionrunTRIMe = System.nanoTime() - selectionStart;

        runTRIMeConsumer.accept(generationrunTRIMe, selectionrunTRIMe);
        return seeds;
    }
}
