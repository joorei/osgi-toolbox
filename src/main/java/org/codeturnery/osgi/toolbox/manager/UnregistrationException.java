package org.codeturnery.osgi.toolbox.manager;

/**
 * Indicates that the throwing bundle could not be unregistrered for some
 * reason, e.g. because an {@link ExpiredException} was thrown.
 */
public class UnregistrationException extends StageChangeException {

	private static final long serialVersionUID = 2243546366688194656L;

	UnregistrationException(final String message) {
		super(message);
	}

	UnregistrationException(final Throwable cause) {
		super(cause);
	}
}
