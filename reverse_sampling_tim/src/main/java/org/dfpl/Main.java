package org.dfpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;

import org.dfpl.element.Edge;
import org.dfpl.element.Graph;
import org.dfpl.element.TimeInstant;
import org.dfpl.element.Vertex;
import org.dfpl.trim.TICSimulator;
import org.dfpl.trim.TRIM;
import org.dfpl.trim.TRRGeneration;
import org.dfpl.trim.ThetaComputation;

public class Main {
    public static void main(String[] args) throws IOException {
        String dataset = args.length > 0 ? args[0] : "bitcoin_alpha.csv";
        int k = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        int runs = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        if (k <= 0 || runs <= 0) {
            throw new IllegalArgumentException("k and runs must be positive");
        }
        int thetaVal = 10000; // Number of samples to find LB
        double epsilon = 0.1;
        double alpha = 0.01;
        int simulations = 1000;
        Graph graph = readGraph(dataset);
        
        System.out.printf("Time range: %s to %s%n", graph.getMinTime(), graph.getMaxTime());
        double totalLB = 0.0;
        double totalTheta = 0.0;
        double totalPreparationRuntime = 0.0;
        double totalThetaRuntime = 0.0;
        double totalGenerationRuntime = 0.0;
        double totalSelectionRuntime = 0.0;
        double totalTimRuntime = 0.0;
        double totalEvaluationRuntime = 0.0;
        double meanInfluence = 0.0;
        double influenceM2 = 0.0;

        System.out.printf("%nParameters: k=%d, thetaVal=%d, epsilon=%.2f, alpha=%.2f%n",
                k, thetaVal, epsilon, alpha);
        for (int run = 0; run < runs; run++) {
            Random random = new Random(42L + run);
            System.out.printf("%nRun %d/%d — Random seed: %d%n",
                    run + 1, runs, 42L + run);
            graph = readGraph(dataset);
            System.out.println("Loaded resource: " + dataset);

            System.out.printf("Graph: %d vertices, %d directed edges, %d edge events%n",
                    graph.getVertices().size(), graph.getEdges().size(), graph.getEdgeEvents().size());
            System.out.printf("Time range: %s to %s%n", graph.getMinTime(), graph.getMaxTime());

            // Rebuild the incoming-event index for every run.
            long preparationStart = System.nanoTime();
            TRRGeneration.PreparedGraph prepared = TRRGeneration.prepare(graph);
            long preparationRuntime = System.nanoTime() - preparationStart;
            System.out.printf("TRR index preparation runtime: %.4f seconds%n",
                    preparationRuntime / 1_000_000_000.0);
            double[] lowerBound = new double[1];
            long thetaStart = System.nanoTime();
            long theta = ThetaComputation.computeTheta(graph, k, thetaVal, epsilon, alpha, random,
                    value -> lowerBound[0] = value, prepared);
            long thetaRuntime = System.nanoTime() - thetaStart;
            System.out.printf("Lower bound (LB): %.4f%n", lowerBound[0]);
            System.out.println("Computed theta: " + theta);
            System.out.printf("Theta computation runtime: %.4f seconds%n", thetaRuntime / 1_000_000_000.0);

            // Continue the same RNG stream and generate fresh final TRR samples.
            long[] stageRuntimes = new long[2];
            long timStart = System.nanoTime();
            Set<Vertex> timSeeds = TRIM.runTRIM(graph, k, theta, random, (generation, selection) -> {
                stageRuntimes[0] = generation;
                stageRuntimes[1] = selection;
            }, prepared);
            long timRuntime = System.nanoTime() - timStart;
            System.out.println("T-IM seeds: " + timSeeds);
            System.out.printf("Final TRR generation runtime: %.4f seconds%n",
                    stageRuntimes[0] / 1_000_000_000.0);
            System.out.printf("Seed selection runtime: %.4f seconds%n",
                    stageRuntimes[1] / 1_000_000_000.0);
            System.out.printf("T-IM runtime (generation + selection): %.4f seconds%n",
                    timRuntime / 1_000_000_000.0);

            // Include preparation; exclude graph loading and Monte Carlo evaluation.
            double algorithmRuntime = (double) preparationRuntime + thetaRuntime + timRuntime;
            System.out.printf("Total algorithm runtime (excluding Monte Carlo): %.4f seconds%n",
                    algorithmRuntime / 1_000_000_000.0);

            long evaluationSeed = 2026L + run;
            Random evaluationRandom = new Random(evaluationSeed);
            long evaluationStart = System.nanoTime();
            double estimatedInfluence = TICSimulator.estimateInfluence(
                    graph, timSeeds, simulations, evaluationRandom);
            long evaluationRuntime = System.nanoTime() - evaluationStart;
            System.out.println("Monte Carlo simulations: " + simulations);
            System.out.println("Estimated influence spread: " + estimatedInfluence);
            System.out.printf("Monte Carlo evaluation runtime: %.4f seconds%n",
                    evaluationRuntime / 1_000_000_000.0);
            totalLB += lowerBound[0];
            totalTheta += theta;
            totalPreparationRuntime += preparationRuntime;
            totalThetaRuntime += thetaRuntime;
            totalGenerationRuntime += stageRuntimes[0];
            totalSelectionRuntime += stageRuntimes[1];
            totalTimRuntime += timRuntime;
            totalEvaluationRuntime += evaluationRuntime;
            double delta = estimatedInfluence - meanInfluence;
            meanInfluence += delta / (run + 1);
            influenceM2 += delta * (estimatedInfluence - meanInfluence);
        }

        double nanosPerSecond = 1_000_000_000.0;
        // Sample standard deviation across independent run estimates.
    
        double influenceStdDev = runs > 1 ? Math.sqrt(Math.max(0.0, influenceM2 / (runs - 1))) : 0.0;
        System.out.printf("%nSummary over %d runs%n", runs);
        System.out.println("k = " + k);
        System.out.printf("Average LB: %.4f%n", totalLB / runs);
        System.out.printf("Average theta: %.0f%n", totalTheta / runs);
        System.out.printf("Average TRR index preparation runtime: %.4f seconds%n",
                totalPreparationRuntime / runs / nanosPerSecond);
        System.out.printf("Average theta computation runtime: %.4f seconds%n",
                totalThetaRuntime / runs / nanosPerSecond);
        System.out.printf("Average final TRR generation runtime: %.4f seconds%n",
                totalGenerationRuntime / runs / nanosPerSecond);
        System.out.printf("Average seed selection runtime: %.4f seconds%n",
                totalSelectionRuntime / runs / nanosPerSecond);
        System.out.printf("Average T-IM runtime (generation + selection): %.4f seconds%n",
                totalTimRuntime / runs / nanosPerSecond);
        double totalAlgorithmRuntime = totalPreparationRuntime + totalThetaRuntime + totalTimRuntime;
        System.out.printf("Total algorithm runtime across all runs (including preparation, excluding graph loading and Monte Carlo): %.4f seconds%n",
                totalAlgorithmRuntime / nanosPerSecond);
        System.out.printf("Average total algorithm runtime (including preparation, excluding graph loading and Monte Carlo): %.4f seconds%n",
                totalAlgorithmRuntime / runs / nanosPerSecond);
        System.out.printf("Average influence spread: %.4f%n", meanInfluence);
        System.out.printf("Standard deviation of influence spread (sample, n-1): %.4f%n",
                influenceStdDev);
        System.out.printf("Average Monte Carlo evaluation runtime: %.4f seconds%n",
                totalEvaluationRuntime / runs / nanosPerSecond);
    }

    /** Reads CSV or whitespace-separated TXT resources: source, target, timestamp.
     * Blank lines and lines starting with # are skipped.
     */
    public static Graph readGraph(String dataset) throws IOException {
        String path = dataset.startsWith("/") ? dataset : "/" + dataset;
        InputStream input = Main.class.getResourceAsStream(path);
        if (input == null) {
            throw new IOException("Graph resource not found on classpath: " + dataset);
        }
        Graph graph = new Graph();
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.contains(",") ? line.split(",", -1) : line.split("\\s+");
                try {
                    if (fields.length < 3) {
                        throw new IllegalArgumentException("Expected source, target, timestamp");
                    }
                    String source = fields[0].trim();
                    String target = fields[1].trim();
                    if (source.isEmpty() || target.isEmpty()) {
                        throw new IllegalArgumentException("Vertex IDs cannot be empty");
                    }
                    long timestamp = Long.parseLong(fields[2].trim());
                    Edge edge = graph.addEdge(graph.addVertex(source), graph.addVertex(target));
                    edge.addEvent(new TimeInstant(timestamp));
                    minTime = Math.min(minTime, timestamp);
                    maxTime = Math.max(maxTime, timestamp);
                } catch (IllegalArgumentException exception) {
                    throw new IOException(dataset + ":" + lineNumber
                            + ": " + exception.getMessage(), exception);
                }
            }
        }
        if (graph.getVertices().isEmpty()) {
            throw new IOException("Graph resource contains no edge events: " + dataset);
        }
        graph.setTimeRange(new TimeInstant(minTime), new TimeInstant(maxTime));
        return graph;
    }

    /** Loads selected zero-based columns; optionally represents each contact in both directions. */
    public static Graph readGraph(String dataset, int sourceColumn, int targetColumn,
                                   int timeColumn, boolean bidirectional) throws IOException {
        if (sourceColumn < 0 || targetColumn < 0 || timeColumn < 0
                || sourceColumn == targetColumn || sourceColumn == timeColumn || targetColumn == timeColumn) {
            throw new IllegalArgumentException("Column indices must be distinct and nonnegative");
        }
        int requiredColumns = Math.max(sourceColumn, Math.max(targetColumn, timeColumn)) + 1;
        String path = dataset.startsWith("/") ? dataset : "/" + dataset;
        InputStream input = Main.class.getResourceAsStream(path);
        if (input == null) {
            throw new IOException("Graph resource not found on classpath: " + dataset);
        }
        Graph graph = new Graph();
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.contains(",") ? line.split(",", -1) : line.split("\\s+");
                try {
                    if (fields.length < requiredColumns) {
                        throw new IllegalArgumentException("Expected at least " + requiredColumns + " columns");
                    }
                    String source = fields[sourceColumn].trim();
                    String target = fields[targetColumn].trim();
                    if (source.isEmpty() || target.isEmpty()) {
                        throw new IllegalArgumentException("Vertex IDs cannot be empty");
                    }
                    long timestamp = Long.parseLong(fields[timeColumn].trim());
                    Edge edge = graph.addEdge(graph.addVertex(source), graph.addVertex(target));
                    edge.addEvent(new TimeInstant(timestamp));
                    if (bidirectional && !source.equals(target)) {
                        graph.addEdge(graph.getVertex(target), graph.getVertex(source))
                                .addEvent(new TimeInstant(timestamp));
                    }
                    minTime = Math.min(minTime, timestamp);
                    maxTime = Math.max(maxTime, timestamp);
                } catch (IllegalArgumentException exception) {
                    throw new IOException(dataset + ":" + lineNumber
                            + ": " + exception.getMessage(), exception);
                }
            }
        }
        if (graph.getVertices().isEmpty()) {
            throw new IOException("Graph resource contains no edge events: " + dataset);
        }
        graph.setTimeRange(new TimeInstant(minTime), new TimeInstant(maxTime));
        return graph;
    }

    // Compatibility entry point for existing CSV callers 
    public static Graph readCsvGraph(String dataset) throws IOException {
        return readGraph(dataset);
    }

    // Creates a sample temporal graph with 10 vertices and 20 directed edges 
    public static Graph createSampleGraph() {
        Graph graph = new Graph();
        for (int id = 1; id <= 10; id++) {
            graph.addVertex(Integer.toString(id));
        }

        // Each row is {source, target, timestamp}. Edit these to try other paths.
        int[][] temporalEdges = {
                {1, 2, 1},
                {1, 3, 2},
                {2, 3, 3},
                {2, 4, 4},
                {3, 4, 5},
                {3, 5, 6},
                {4, 5, 7},
                {4, 6, 8},
                {5, 6, 9},
                {5, 7, 10},
                {6, 7, 11},
                {6, 8, 12},
                {7, 8, 13},
                {7, 9, 14},
                {8, 9, 15},
                {8, 10, 16},
                {9, 10, 17},
                {10, 1, 18},
                {9, 2, 19},
                {10, 3, 20}
        };

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        for (int[] row : temporalEdges) {
            Edge edge = graph.addEdge(graph.getVertex(Integer.toString(row[0])),
                    graph.getVertex(Integer.toString(row[1])));
            edge.addEvent(new TimeInstant(row[2]));
            minTime = Math.min(minTime, row[2]);
            maxTime = Math.max(maxTime, row[2]);
        }
        graph.setTimeRange(new TimeInstant(minTime), new TimeInstant(maxTime));
        return graph;
    }
}
