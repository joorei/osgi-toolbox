package org.codeturnery.osgi.toolbox.manager;

/**
 * Indicates that the throwing bundle could not be started for some reason,
 * e.g. because it was already started (i.e. is an instance of
 * {@link StartedBundle}) or because an {@link ExpiredException} was thrown.
 */
public class StartException extends StageChangeException {

	private static final long serialVersionUID = -8750543591009554940L;

	StartException(final String message) {
		super(message);
	}

	StartException(final Throwable cause) {
		super(cause);
	}
}
