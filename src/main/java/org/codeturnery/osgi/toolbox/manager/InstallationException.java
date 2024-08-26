package org.codeturnery.osgi.toolbox.manager;

/**
 * Indicates that the throwing bundle could not be installed for some reason,
 * e.g. because it was already installed (i.e. is an instance of
 * {@link InstalledBundle}) or because an {@link ExpiredException} was thrown.
 */
public class InstallationException extends StageChangeException {

	private static final long serialVersionUID = -7948525914759589628L;

	InstallationException(final String message) {
		super(message);
	}

	InstallationException(final Throwable cause) {
		super(cause);
	}
}
