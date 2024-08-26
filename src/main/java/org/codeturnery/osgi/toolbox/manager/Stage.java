package org.codeturnery.osgi.toolbox.manager;

/**
 * The stages a bundle can be in.
 * <p>
 * This information is needed additionally to the type information of
 * {@link RegisteredBundle}, {@link InstalledBundle} and {@link StartedBundle}.
 * For example a {@link StartedBundle} is also of type {@link InstalledBundle}
 * and {@link RegisteredBundle}, but if it was not stopped its stage is
 * {@link Stage#STARTED}.
 */
public enum Stage {
	/**
	 * The bundle was previously at least registered, but is now removed from the
	 * registry and can no longer be used.
	 */
	UNREGISTERED,
	/**
	 * The bundle is registered but was not yet installed or has been uninstalled.
	 */
	REGISTERED,
	/**
	 * The bundle is registered and has been installed, but was not yet started or
	 * has been stopped.
	 */
	INSTALLED,
	/**
	 * The bundle is registered, installed and started.
	 */
	STARTED
}
