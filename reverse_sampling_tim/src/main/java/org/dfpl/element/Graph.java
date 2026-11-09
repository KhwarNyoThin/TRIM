package org.dfpl.element;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Graph {
    private HashMap<String, Vertex> vertices;
    private HashMap<String, Edge> edges;
    private HashMap<String, HashSet<Edge>> outEdges;
    private HashMap<String, HashSet<Edge>> inEdges;
	private TimeInstant minTime = new TimeInstant(Long.MAX_VALUE);
	private TimeInstant maxTime = new TimeInstant(Long.MIN_VALUE);
	// private HashMap<String, HashSet<EdgeEvent>> inEdgeEvents;
	// private HashSet<EdgeEvent> edgeEvents;
	

    public Graph() {
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
		// this.inEdgeEvents = new HashMap<>();
		// this.edgeEvents = new HashSet<>();
    }

	public void setTimeRange(TimeInstant min, TimeInstant max) {
		this.minTime = min;
		this.maxTime = max;
	}

	public TimeInstant getMinTime() {
		return minTime;
	}

	public TimeInstant getMaxTime() {
		return maxTime;
	}

    public Vertex addVertex(String id) {
		if (id.contains("|"))
			throw new IllegalArgumentException("Vertex ID cannot contain '|'");

		return vertices.compute(id, (String identifier, Vertex existingVertex) -> {
			if (existingVertex == null)
				return new Vertex(id);
			return existingVertex;
		});
        // Vertex vertex = new Vertex(id);
        // vertices.put(id, vertex);
        // outEdges.put(id, new HashSet<>());
        // inEdges.put(id, new HashSet<>());
        // return vertex;
    }

    public Vertex getVertex(String id) {
        return vertices.get(id);
    }

    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    public Edge addEdge(Vertex from, Vertex to) {
        
        String edgeId = Edge.getEdgeId(from, to);
        if (edges.containsKey(edgeId)) {
            return edges.get(edgeId);
        }
        
        Edge edge = new Edge(from, to);
        edges.put(edgeId, edge);

        outEdges.compute(from.getId(), (String out, HashSet<Edge> outEdges) -> {
			if (outEdges == null)
				outEdges = new HashSet<>();
			outEdges.add(edge);
			return outEdges;
		});

		inEdges.compute(to.getId(), (String in, HashSet<Edge> inEdges) -> {
			if (inEdges == null)
				inEdges = new HashSet<>();
			inEdges.add(edge);
			return inEdges;
		});
        return edge;
    }

    public Edge getEdge(Vertex from, Vertex to) {
        return edges.get(Edge.getEdgeId(from, to));
    }

    public Collection<Edge> getEdges() {
        return edges.values();
    }

    public void removeVertex(Vertex vertex) {
		String id = vertex.getId();
		this.vertices.remove(id);

		// update a key-value map for edges
		Iterator<Entry<String, Edge>> eIter = edges.entrySet().iterator();
		while (eIter.hasNext()) {
			Entry<String, Edge> entry = eIter.next();
			Edge cEdge = entry.getValue();
			if (cEdge.getVertex(Direction.OUT).equals(vertex) || cEdge.getVertex(Direction.IN).equals(vertex)) {
				eIter.remove();
			}
		}
		// indexes
		Iterator<Entry<String, HashSet<Edge>>> outIter = outEdges.entrySet().iterator();
		while (outIter.hasNext()) {
			Entry<String, HashSet<Edge>> entry = outIter.next();
			if (entry.getKey().equals(id)) {
				outIter.remove();
			} else {
				entry.getValue().removeIf(e -> e.getVertex(Direction.IN).equals(vertex));
				if (entry.getValue().isEmpty())
					outIter.remove();
			}
		}
		Iterator<Entry<String, HashSet<Edge>>> inIter = inEdges.entrySet().iterator();
		while (inIter.hasNext()) {
			Entry<String, HashSet<Edge>> entry = inIter.next();
			if (entry.getKey().equals(id)) {
				inIter.remove();
			} else {
				entry.getValue().removeIf(e -> e.getVertex(Direction.OUT).equals(vertex));
				if (entry.getValue().isEmpty())
					inIter.remove();
			}
		}
	}

    public void removeEdge(Edge edge) {
		this.edges.remove(edge.toString());
		this.outEdges.values().forEach(set -> set.remove(edge));
		this.inEdges.values().forEach(set -> set.remove(edge));
	}

    HashMap<String, HashSet<Edge>> getOutEdges() {
		return outEdges;
	}

	void setOutEdges(HashMap<String, HashSet<Edge>> outEdges) {
		this.outEdges = outEdges;
	}

	HashMap<String, HashSet<Edge>> getInEdges() {
		return inEdges;
	}

	void setInEdges(HashMap<String, HashSet<Edge>> inEdges) {
		this.inEdges = inEdges;
	}

	public HashSet<Edge> getOutEdges(Vertex vertex) {
		return outEdges.get(vertex.getId());
	}

	public HashSet<Edge> getInEdges(Vertex vertex) {
		return inEdges.get(vertex.getId());
	}

	/**
	 * Returns the activation probability for an edge event:
	 * p(u, v, t) = 1 / d_in(v).
	 */
	public double getPropagationProbability(EdgeEvent edgeEvent) {
		Objects.requireNonNull(edgeEvent, "edgeEvent");
		Vertex target = edgeEvent.getVertex(Direction.IN);
		HashSet<Edge> incomingEdges = inEdges.get(target.getId());
		if (incomingEdges == null || incomingEdges.isEmpty()) {
			throw new IllegalArgumentException(
					"Edge event target has no incoming edges: " + edgeEvent
			);
		}
		return 1.0 / incomingEdges.size();
	}

	public HashSet<Edge> getOutEdges(Vertex vertex, TimeInstant time) {
		if (outEdges.get(vertex.getId()) == null) {
			return new HashSet<>();
		}
		return outEdges.get(vertex.getId()).stream().filter(e -> 
			e.getEvent(time) != null).collect(Collectors.toCollection(HashSet::new));
	}

	public HashSet<Edge> getInEdges(Vertex vertex, TimeInstant time) {
		if (inEdges.get(vertex.getId()) == null) {
			return new HashSet<>();
		}
		return inEdges.get(vertex.getId()).stream().filter(e -> 
			e.getEvent(time) != null).collect(Collectors.toCollection(HashSet::new));
	}

    public Collection<EdgeEvent> getEdgeEvents() {
		return getEdges().parallelStream().flatMap(e -> e.getEvents().parallelStream()).collect(Collectors.toSet());
	}

	public Collection<EdgeEvent> getInEdgeEvents(Vertex vertex) {
		HashSet<Edge> incomingEdges = inEdges.get(vertex.getId());
		if (incomingEdges == null) {
			return Collections.emptySet();
		}

		// Flattens all events from every edge pointing to this vertex
		return incomingEdges.stream()
				.flatMap(edge -> edge.getEvents().stream())
				.collect(Collectors.toSet());
	}

	public Collection<EdgeEvent> getOutEdgeEvents(Vertex vertex) {
		HashSet<Edge> outgoingEdges = outEdges.get(vertex.getId());
		if (outgoingEdges == null) {
			return Collections.emptySet();
		}

		// Flattens all events from every edge starting from this vertex
		return outgoingEdges.stream()
				.flatMap(edge -> edge.getEvents().stream())
				.collect(Collectors.toSet());
	}
}

