package org.dfpl.element;

public enum Direction {

	OUT, IN, BOTH;

	public static final Direction[] proper = new Direction[] { OUT, IN };

	public Direction opposite() {
		if (this.equals(OUT))
			return IN;
		else if (this.equals(IN))
			return OUT;
		else
			return BOTH;
	}
}
