package org.codeturnery.osgi.toolbox.manager;

/**
 * Indicates that the throwing bundle could not be uninstalled for some reason,
 * e.g. because an {@link ExpiredException} was thrown.
 */
public class UninstallationException extends StageChangeException {

	private static final long serialVersionUID = -8846790804082750271L;

	UninstallationException(final String message) {
		super(message);
	}

	UninstallationException(final Throwable cause) {
		super(cause);
	}
}
