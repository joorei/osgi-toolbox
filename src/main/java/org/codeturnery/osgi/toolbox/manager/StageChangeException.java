package org.codeturnery.osgi.toolbox.manager;

abstract public class StageChangeException extends Exception {

	private static final long serialVersionUID = -3704305360970855725L;

	StageChangeException(final String message) {
		super(message);
	}

	StageChangeException(final Throwable cause) {
		super(cause);
	}
}
