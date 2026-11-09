package org.dfpl.element;

import java.util.Objects;

public class VertexEvent implements Comparable<VertexEvent> {
    
    private final Vertex vertex;
    private final Time time;

    public VertexEvent(Vertex vertex, Time time) {
        this.vertex = vertex;
        this.time = time;
    }

    public Time getTime() {
        return this.time;
    }

    public Vertex getVertex() {
        return this.vertex;
    }

    public int compareTo(VertexEvent event) {
        int timeCompare = this.time.compareTo(event.getTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        return this.vertex.getId().compareTo(event.getVertex().getId());
    }

    public String getId() {
        return vertex.getId() + " at " + time.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VertexEvent event)) {
            return false;
        }
        return vertex.equals(event.getVertex()) && time.equals(event.getTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertex, time);
    }

    public String toString() {
        return vertex.toString() + "@" + time.toString();
    }
}
