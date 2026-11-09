package org.dfpl.trim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;

import org.dfpl.element.Vertex;

public final class SeedSelection {
    private SeedSelection() {}

    // Seed selection with greedy maximum coverage
    public static Set<Vertex> selectSeeds(Collection<Vertex> vertices,
                                          List<Set<Vertex>> trrSets, int k) {
        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(trrSets, "trrSets");
        List<Vertex> candidates = new ArrayList<>(new HashSet<>(vertices));
        candidates.sort(Comparator.comparing(Vertex::getId));
        int n = candidates.size();
        if (k < 0 || k > n) {
            throw new IllegalArgumentException("k must be between 0 and the number of distinct vertices");
        }
        Set<Vertex> seeds = new LinkedHashSet<>();
        if (k == 0) {
            return seeds;
        }

        Map<Vertex, Integer> candidateIndices = new HashMap<>();
        for (int u = 0; u < n; u++) {
            candidateIndices.put(candidates.get(u), u);
        }

        // Count memberships, then allocate compact primitive inverted-index lists
        int[] gain = new int[n];
        for (Set<Vertex> sample : trrSets) {
            for (Vertex vertex : sample) {
                Integer u = candidateIndices.get(vertex);
                if (u != null) {
                    gain[u]++;
                }
            }
        }
        int[][] memberships = new int[n][];
        for (int u = 0; u < n; u++) {
            memberships[u] = new int[gain[u]];
        }
        int[] positions = new int[n];
        int sampleId = 0;
        for (Set<Vertex> sample : trrSets) {
            for (Vertex vertex : sample) {
                Integer u = candidateIndices.get(vertex);
                if (u != null) {
                    memberships[u][positions[u]++] = sampleId;
                }
            }
            sampleId++;
        }
        // Constant-time access even when the caller supplies a LinkedList
        List<Set<Vertex>> samples = trrSets instanceof RandomAccess
                ? trrSets : new ArrayList<>(trrSets);
        boolean[] covered = new boolean[samples.size()];
        boolean[] selected = new boolean[n];
        while (seeds.size() < k) {
            int best = -1;
            for (int u = 0; u < n; u++) {
                // Candidates are sorted with ID
                if (!selected[u] && (best == -1 || gain[u] > gain[best])) {
                    best = u;
                }
            }
            selected[best] = true;
            seeds.add(candidates.get(best));
            for (int id : memberships[best]) {
                if (covered[id]) {
                    continue;
                }
                covered[id] = true;
                for (Vertex vertex : samples.get(id)) {
                    Integer u = candidateIndices.get(vertex);
                    if (u != null && !selected[u]) {
                        gain[u]--;
                    }
                }
            }
        }
        return seeds;
    }
}
