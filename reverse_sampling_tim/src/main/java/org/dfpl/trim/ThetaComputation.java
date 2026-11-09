package org.dfpl.trim;

import org.dfpl.element.Graph;
import org.dfpl.element.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleConsumer;

public final class ThetaComputation {
    private ThetaComputation() {}

    /**
     * Computes the Section 5 lower bound and final sample count,
     * Uses exactly thetaVal preliminary and thetaVal samples to compute LB
     *
     * @throws IllegalArgumentException if the input parameters are invalid
     * @throws ArithmeticException if the lower bound or final sample count is invalid
     *         or the sample count cannot be represented as a long
     */
    public static long computeTheta(Graph graph, int k, int thetaVal,
                                           double epsilon, double alpha, Random random) {
        return computeTheta(graph, k, thetaVal, epsilon, alpha, random, lowerBound -> {});
    }

    // Reports the computed lower bound
    public static long computeTheta(Graph graph, int k, int thetaVal,
                                    double epsilon, double alpha, Random random,
                                    DoubleConsumer lowerBoundConsumer) {
        return computeTheta(graph, k, thetaVal, epsilon, alpha, random,
                lowerBoundConsumer, TRRGeneration.prepare(graph));
    }

    // Uses one caller-prepared index for preliminary and validation samples
    public static long computeTheta(Graph graph, int k, int thetaVal,
                                    double epsilon, double alpha, Random random,
                                    DoubleConsumer lowerBoundConsumer,
                                    TRRGeneration.PreparedGraph prepared) {
        Objects.requireNonNull(prepared, "prepared").requireGraph(graph);
        Objects.requireNonNull(lowerBoundConsumer, "lowerBoundConsumer");
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(random, "random");
        int n = graph.getVertices().size();
        if (k <= 0 || k > n) {
            throw new IllegalArgumentException("k must satisfy 0 < k <= number of vertices");
        }
        if (thetaVal <= 0) {
            throw new IllegalArgumentException("thetaVal must be positive");
        }
        if (!(epsilon > 0.0 && epsilon < 1.0)) {
            throw new IllegalArgumentException("epsilon must satisfy 0 < epsilon < 1");
        }
        if (!(alpha > 0.0 && alpha < 1.0)) {
            throw new IllegalArgumentException("alpha must satisfy 0 < alpha < 1");
        }

        List<Set<Vertex>> preliminary = new ArrayList<>();
        for (int i = 0; i < thetaVal; i++) {
            preliminary.add(TRRGeneration.generateTRR(prepared, random));
        }
        Set<Vertex> seeds = SeedSelection.selectSeeds(graph.getVertices(), preliminary, k);

        long coverage = 0;
        for (int i = 0; i < thetaVal; i++) {
            Set<Vertex> sample = TRRGeneration.generateTRR(prepared, random);
            if (!Collections.disjoint(seeds, sample)) {
                coverage++;
            }
        }
        double influenceEstimate = (double) n * coverage / thetaVal;

        // alphaVal = alphaMain = alpha / 2 
        double logValidation = Math.log(2.0) - Math.log(alpha); // ln(1 / alphaVal)
        double deviation = n * Math.sqrt(logValidation / (2.0 * thetaVal));
        double lowerBound = Math.max((double) k, influenceEstimate - deviation);
        if (!Double.isFinite(lowerBound) || lowerBound <= 0.0) {
            throw new ArithmeticException("Computed lower bound must be positive and finite");
        }

        double factor = 2.0 - 1.0 / Math.E;
        double logMain = Math.log(4.0) - Math.log(alpha); // ln(2 / alphaMain)
        double complexity = logMain + k * Math.log(Math.E * n / k);
        // Equivalent to 3*n*factor^2/(epsilon^2*LB); successive divisions
        // avoid prematurely underflowing epsilon^2. Round only the final value.
        double required = (3.0 * n * factor * factor / lowerBound)
                / epsilon / epsilon * complexity;
        double rounded = Math.ceil(required);
        // (double) Long.MAX_VALUE rounds UP to 2^63, so use an exclusive bound.
        if (!Double.isFinite(rounded) || rounded >= 0x1.0p63) {
            throw new ArithmeticException("Computed theta exceeds Long.MAX_VALUE");
        }
        if (rounded <= 0.0) {
            throw new ArithmeticException("Computed theta must be positive");
        }
        lowerBoundConsumer.accept(lowerBound);
        return (long) rounded;
    }
}
