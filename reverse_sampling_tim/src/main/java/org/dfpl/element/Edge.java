package org.dfpl.element;

import java.util.NavigableSet;
import java.util.TreeSet;


public class Edge {
	private Vertex from;
	private Vertex to;
	private NavigableSet<EdgeEvent> events;
	private String id;
	// private static int edgeId = 0;

    public Edge(Vertex from, Vertex to) {
        this.from = from;
        this.to = to;
        this.id = getEdgeId(from, to);
        this.events = new TreeSet<>();
    }

    public static String getEdgeId (Vertex from, Vertex to) {
        return from.getId() + "|" + to.getId();
    }

    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
		if (direction.equals(Direction.OUT)) {
			return from;
		} else if (direction.equals(Direction.IN)) {
			return to;
		} else {
			throw new IllegalArgumentException("A direction should be either OUT or IN");
		}
	}


    public EdgeEvent addEvent(TimeInstant time) {
        EdgeEvent event = new EdgeEvent(this, time);
        this.events.add(event);
        return event;
    }

    public EdgeEvent getEvent(Time time) {
		for (EdgeEvent event : this.events) {
			if (event.getTime().equals(time))
				return event;
		}
		return null;
	}

	public NavigableSet<EdgeEvent> getEvents() {
		return events;
	}

    public NavigableSet<EdgeEvent> getEvents(Time time) {
		NavigableSet<EdgeEvent> validEvents = new TreeSet<>();

        this.events.stream().filter(edgeEvent -> {
			return time.equals(edgeEvent.getTime());
		}).forEach(edgeEvent -> {
			validEvents.add(edgeEvent);
		});

		return validEvents;
    }

    public void removeEvent(EdgeEvent event) {
        this.events.remove(event);
    }

    public void removeEvents(Time time) {
        this.events.removeIf(event -> event.getTime().equals(time));
    }
	public int hashCode() {
		return id.hashCode();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Edge))
			return false;
		return id.equals(obj.toString());
	}

	public String toString() {
		return id;
	}

	public String getId() {
		return id;
	}

	public Vertex getOutgoingVertex() {
		return from;
	}

	public Vertex getIncomingVertex() {
		return to;
	}

}
