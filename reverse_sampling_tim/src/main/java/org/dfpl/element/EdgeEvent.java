package org.dfpl.element;

import java.util.Objects;

public class EdgeEvent implements Comparable<EdgeEvent> {
    private final Edge edge;
    private final TimeInstant time;

    public EdgeEvent(Edge edge, TimeInstant time) {
        this.edge = edge;
        this.time = time;
    }

    public VertexEvent getVertexEvent(Direction direction) {
        return edge.getVertex(direction).getEvent(time);
    }

    public TimeInstant getTime() {
        return this.time;
    }

    public Edge getEdge() {
        return this.edge;
    }

    public int compareTo(EdgeEvent event) {
        int timeCompare = this.time.compareTo(event.getTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        return this.edge.getId().compareTo(event.getEdge().getId());
    }

    public String getId() {
        return edge.getId() + "@" + time.toString();
    }

    public Vertex getVertex(Direction direction) {
        return edge.getVertex(direction);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EdgeEvent event)) {
            return false;
        }
        return edge.equals(event.getEdge()) && time.equals(event.getTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(edge, time);
    }

    public String toString() {
        return edge.toString() + "@" + time.toString();
    }
}
