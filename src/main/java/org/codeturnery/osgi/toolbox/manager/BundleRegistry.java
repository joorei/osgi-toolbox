package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.util.List;

public interface BundleRegistry {
	/**
	 * Returns the {@link List} of non-expired bundles sorted by their
	 * {@link RegisteredBundle#getIndex() indices}.
	 * <p>
	 * The content and positions in the returned {@link List} will automatically be
	 * updated when bundles are added, removed or their index updated.
	 * <p>
	 * While the returned {@link List} is just a view of the registry's state and
	 * not modifiable, its entries (the bundles) are the actual instances.
	 * 
	 * @return The set of currently registered bundles. Entries may not just be
	 *         {@link Stage#REGISTERED} but {@link Stage#INSTALLED} or
	 *         {@link Stage#STARTED} as well.
	 */
	public List<RegisteredBundle> getBundles();

	/**
	 * {@link Stage#REGISTERED registers} a new bundle from the given
	 * {@code jarFile}.
	 * <p>
	 * The given {@code jarFile} must not be currently used for an existing bundle.
	 *
	 * @param jarFile
	 * @return A {@link Stage#REGISTERED} bundle, not {@link Stage#INSTALLED} or
	 *         {@link Stage#STARTED}.
	 * @throws RegistrationException
	 */
	public RegisteredBundle registerBundle(final File jarFile) throws RegistrationException;

	/**
	 * Creates a list of proxies for the services provided by all
	 * {@link Stage#STARTED} bundles, that were exposed via the given interface.
	 * <p>
	 * The services are not exposed directly because references to the actual
	 * services must be handled carefully for the garbage collector to be able to
	 * clean them up.
	 * 
	 * @param <T>
	 * @param type
	 * @return A list of proxies that can be used to call the corresponding
	 *         services.
	 * @throws LoadServiceException
	 */
	public <T> List<? extends ServiceWrapper<T>> loadServices(final Class<T> type) throws LoadServiceException;

	public boolean isNotInstalled(final RegisteredBundle bundle);

	public boolean isInstalledButNotStarted(final RegisteredBundle bundle);

	public boolean isStarted(final RegisteredBundle bundle);
}
