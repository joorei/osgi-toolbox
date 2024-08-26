package org.codeturnery.osgi.toolbox.manager;

import java.util.Optional;

/**
 * A set of Java classes and functionality backed by an OSGi JAR file that can
 * be installed and uninstalled during runtime.
 * <p>
 * A bundle can be known in the application in different stages
 * ({@link RegisteredBundle registered}, {@link InstalledBundle installed} and
 * {@link StartedBundle started}. Please note that altering or removing the
 * backing JAR file while the file is known in any way in the application (even
 * if it is just {@link RegisteredBundle registered}) will result in undefined
 * behavior.
 * 
 * <h2>Conflict potential</h2>
 * <p>
 * {@link InstalledBundle}s can potentially conflict with each other if they
 * define classes with equal fully qualified names but different
 * implementations. To reduce the likelihood of such conflicts, before allowing
 * the installation of a bundle its JAR file is scanned for FQCNs that are
 * already defined by another bundle. The affected bundle will be marked as
 * conflicting. Installing conflicting bundles anyway will result in undefined
 * behavior.
 * <p>
 * However, scanning the JAR file is not bulletproof and even if unlikely two
 * bundles can still conflict with each other, e.&nbsp;g. due to conflicting
 * dependencies. In such case they may cause problems at runtime.
 * <p>
 * A more specific conflict is the case of two bundles that use the same
 * symbolic name. When combined with the corresponding bundle version, symbolic
 * names are supposed to be unique between different bundles. Hence, when two
 * bundles use the same symbolic name (not to be confused with their file name)
 * it can be assumed that they will conflict with each other and they are marked
 * accordingly. A likely cause of such case is the attempted usage of basically
 * the same bundle in two different versions.
 * <p>
 * Loading the same JAR path for different bundles into the application is not
 * allowed.
 */
public interface BundleInterface {
	/**
	 * @return The symbolic name of this bundle. Supposedly unique.
	 */
	public String getSymbolicName();
	public Version getVersion();
	public boolean isExpired();
	public Optional<Expiration> getExpiration();
}
