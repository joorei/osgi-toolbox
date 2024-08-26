package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;

/**
 * If this instance hasn't been expired it represents a bundle that is made
 * known to the application but <strong>may</strong> not be
 * {@link Stage#INSTALLED} (instance of {@link InstalledBundle}) or
 * {@link Stage#STARTED} (instance of {@link StartedBundle}).
 * <p>
 * Instances expire when they switch their stage, i.&nbsp;e. are installed,
 * started, stopped, uninstalled or unregistered. Corresponding methods to
 * switch stages will return a new instance with the new stage after which the
 * old instance should no longer be accessed.
 * <p>
 * A {@link RegisteredBundle} provides information about the bundle but if it
 * isn't an {@link InstalledBundle} too then there is no potential for conflicts
 * with other bundles and even though it is visible in the application, its
 * functionality is neither available nor has it effect on the application.
 * <p>
 * Changing or deleting the JAR file backing this instance is
 * <strong>not</strong> allowed and will result in undefined behavior.
 */
public interface RegisteredBundle extends BundleInterface {

	public URI getUri();

	public Map<String, JarEntry> getClassEntries();

	public boolean isRegisteredFrom(final File jarFile);

	public Instant getRegistrationTime();

	public int getIndex();

	/**
	 * @return A new instance to be used for all further actions on this bundle.
	 *         The current instance will be set as expired and must not be used
	 *         anymore.
	 *         
	 * @throws InstallationException if a problem occurred while installing the bundle
	 */
	public InstalledBundle install() throws InstallationException;

	/**
	 * The current instance will be set as expired and must not be used anymore.
	 * 
	 * @throws UnregistrationException if a problem occurred while unregistering the bundle
	 */
	public void unregister() throws UnregistrationException;

	/**
	 * Two possibilities how this bundle may conflict with any other currently
	 * {@link Stage#REGISTERED} bundle are checked:
	 * <ol>
	 * <li>if they both use the same symbolic name</li>
	 * <li>if they both define classes that match in their fully qualified class
	 * name but not in their CRC content checksum</li>
	 * </ol>
	 * 
	 * @return All (at the moment of invocation non-expired) bundles that have
	 *         potential to conflict with the current bundle.
	 */
	public Set<BundleConflict> getConflicts();

	public String getSymbolicNameWithVersion();
}
