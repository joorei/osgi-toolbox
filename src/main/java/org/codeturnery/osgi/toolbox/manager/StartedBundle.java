package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

public interface StartedBundle extends InstalledBundle {
	public Instant getStartTime();

	/**
	 * @return A new instance to be used for all further actions on this bundle.
	 *         The current instance will be set as expired and must not be used
	 *         anymore.
	 *         
	 * @throws StopException if a problem occurred while stopping the bundle
	 */
	public InstalledBundle stop() throws StopException;
}
