package org.dfpl.element;

import java.util.*;
import java.util.stream.Collectors;

public class Vertex {
    private Graph g;
    private String id;
    // private TreeSet<VertexEvent> events;

    public Vertex(String id) {
        this.id = id;
		// this.events = new TreeSet<>();
    }

    public Collection<Edge> getEdges(Direction direction, List<String> labels) {
		HashMap<String, HashSet<Edge>> edgeSet = null;
		if (direction.equals(Direction.OUT))
			edgeSet = g.getOutEdges();
		else if (direction.equals(Direction.IN))
			edgeSet = g.getInEdges();

		if (edgeSet == null || !edgeSet.containsKey(id))
			return new HashSet<>();

		return edgeSet.get(id).parallelStream().collect(Collectors.toSet());

	}

    public Collection<Vertex> getVertices(Direction direction, List<String> labels) {
		HashMap<String, HashSet<Edge>> edgeSet = null;
		if (direction.equals(Direction.OUT))
			edgeSet = g.getOutEdges();
		else if (direction.equals(Direction.IN))
			edgeSet = g.getInEdges();

		if (edgeSet == null || !edgeSet.containsKey(id))
			return new HashSet<>();
		return edgeSet.get(id).parallelStream().map(e -> e.getVertex(direction.opposite())).collect(Collectors.toSet());
	}

	public VertexEvent getEvent(Time time) {
		return new VertexEvent(this, time);
	}

    public String getId() {
        return id;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object obj) {
		if (!(obj instanceof Vertex))
			return false;
		return this.getId().equals(((Vertex) obj).getId());
	}

	public String toString() {
		return id;
	}
}

