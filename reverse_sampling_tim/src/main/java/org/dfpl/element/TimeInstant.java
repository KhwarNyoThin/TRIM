package org.dfpl.element;

import java.util.Objects;

public class TimeInstant implements Time {
	protected long time;

	public TimeInstant(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return Long.toString(time);
	}

	@Override
	public long getStartTime() {
		return time;
	}

	@Override
	public long getFinishTime() {
		return time;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public boolean equals(Object o) { 
		if (!(o instanceof TimeInstant oTime))
			return false;
		return time == oTime.getTime();
	}

	@Override
	public int hashCode() {
		return Objects.hash(time);
	}

	public TimeInstant next() {
        return new TimeInstant(this.time + 1);
    }

	@Override
	public long getDuration() {
		return 0;
	}

	public int compare(TimeInstant other) {
        return Long.compare(this.time, other.time);
    }
}


