package org.codeturnery.osgi.toolbox.manager;

/**
 * Indicates that the throwing bundle could not be stopped for some reason,
 * e.g. because an {@link ExpiredException} was thrown.
 */
public class StopException extends StageChangeException {

	private static final long serialVersionUID = -5237856821150142130L;

	StopException(final String message) {
		super(message);
	}

	StopException(final Throwable cause) {
		super(cause);
	}
}
