package org.dfpl.element;

public interface Time extends Comparable<Time> {
    default int compareTo(Time o) {
        if (this.getTime() < o.getTime())
			return -1;
		else if (this.getTime() > o.getTime())
			return +1;
		else
			return 0;
    }

    /**
	 * return a time in time-instant or a start time in time-period used for
	 * compareTo, due to disjointness, it is not ambiguous.
	 *
	 * @return a time in time-instant or a start time in time-period
	 */
	public long getTime();

	/**
	 * @return a time in time-instant or a start time in time-period
	 */
	public long getStartTime();

	/**
	 * @return a time in time-instant or a finish time in time-period
	 */
	public long getFinishTime();

	/**
	 * @return finishTime - startTime
	 */
	public long getDuration();

}
