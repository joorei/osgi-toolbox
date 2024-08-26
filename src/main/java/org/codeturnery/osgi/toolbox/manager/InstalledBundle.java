package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

public interface InstalledBundle extends RegisteredBundle {
	public Instant getInstallationTime();

	/**
	 * @return A new instance to be used for all further actions on this bundle.
	 *         The current instance will be set as expired and must not be used
	 *         anymore.
	 *         
	 * @throws UninstallationException if a problem occurred while uninstalling the bundle
	 */
	public RegisteredBundle uninstall() throws UninstallationException;

	/**
	 * @return A new instance to be used for all further actions on this bundle.
	 *         The current instance will be set as expired and must not be used
	 *         anymore.
	 *         
	 * @throws StartException if a problem occurred while starting the bundle
	 */
	public StartedBundle start() throws StartException;
}
