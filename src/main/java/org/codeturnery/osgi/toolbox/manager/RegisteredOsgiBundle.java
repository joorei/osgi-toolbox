package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;

class RegisteredOsgiBundle implements RegisteredBundle {
	private static final Attributes.Name SYMBOLIC_NAME = new Attributes.Name("Bundle-SymbolicName"); //$NON-NLS-1$
	private static final Attributes.Name VERSION = new Attributes.Name("Bundle-Version"); //$NON-NLS-1$
	private static final Attributes.Name BUNDLE_ACTIVATOR = new Attributes.Name("Bundle-Activator"); //$NON-NLS-1$
	/**
	 * The registry this bundle was registered into.
	 */
	protected final OsgiBundleRegistry bundleRegistry;

	private Optional<Expiration> expiration = Checks.requireNonNull(Optional.empty());

	/**
	 * File URI (derived from the initial JAR path). Needed to install the bundle.
	 */
	protected final URI uri;
	/**
	 * The manifest information parsed from the <code>META-INF/MANIFEST.MF</code> in
	 * the bundles JAR file.
	 */
	private final Manifest manifest;
	/**
	 * The class names defined by the bundles JAR file.
	 */
	private final Map<String, JarEntry> classEntries;

	private final Version version;
	private final Instant registrationTime;

	RegisteredOsgiBundle(final File jarFile, final OsgiBundleRegistry bundleRegistry) throws RegistrationException {
		try {
			if (!jarFile.canRead()) {
				throw new RegistrationException("Can't read file in path: " + jarFile.getAbsolutePath());
			}
			try (final JarFile jar = new JarFile(jarFile, true);) {
				this.manifest = Checks.requireNonNull(jar.getManifest());
				this.classEntries = determineClassEntries(jar);
			}
			throwIfInvalid();
			this.uri = Checks.requireNonNull(jarFile.getAbsoluteFile().toURI());
			this.version = new Version(
					getNameAttribute(Checks.requireNonNull(this.manifest.getMainAttributes()), VERSION));
			// TODO: set up a listener watching the file for changes
			// TODO: check readability/file type
			this.registrationTime = Checks.requireNonNull(Instant.now());
			this.bundleRegistry = bundleRegistry;
			final Optional<RegisteredOsgiBundle> existingBundle = bundleRegistry.getBundleRegisteredFrom(jarFile);
			if (existingBundle.isPresent()) {
				throw new BundleAlreadyRegisteredException(jarFile, existingBundle.get());
			}
			bundleRegistry.bundles.add(this);
		} catch (final RegistrationException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new RegistrationException(exception);
		}
	}

	protected RegisteredOsgiBundle(final RegisteredOsgiBundle bundle) {
		this.uri = bundle.uri;
		this.manifest = bundle.manifest;
		this.classEntries = bundle.classEntries;
		this.version = bundle.version;
		this.registrationTime = bundle.registrationTime;
		this.bundleRegistry = bundle.bundleRegistry;
	}

	@Override
	public Map<String, JarEntry> getClassEntries() throws ExpiredException {
		throwIfExpired();
		return this.classEntries;
	}

	@Override
	public String getSymbolicName() throws ExpiredException {
		throwIfExpired();
		return getNameAttribute(Checks.requireNonNull(this.manifest.getMainAttributes()), SYMBOLIC_NAME);
	}

	@Override
	public URI getUri() throws ExpiredException {
		throwIfExpired();
		return this.uri;
	}

	/**
	 * @return The version of this bundle.
	 */
	@Override
	public Version getVersion() throws ExpiredException {
		throwIfExpired();
		return this.version;
	}

	@Override
	public boolean isRegisteredFrom(final File jarFile) throws ExpiredException {
		throwIfExpired();
		return this.uri.equals(jarFile.getAbsoluteFile().toURI());
	}

	@Override
	public boolean isExpired() {
		return this.expiration.isPresent();
	}

	@Override
	public Optional<Expiration> getExpiration() {
		return this.expiration;
	}

	@Override
	public Instant getRegistrationTime() throws ExpiredException {
		throwIfExpired();
		return this.registrationTime;
	}

	@Override
	public int getIndex() throws ExpiredException {
		throwIfExpired();
		return this.bundleRegistry.getBundleIndex(this);
	}

	@Override
	public InstalledBundle install() throws InstallationException {
		try {
			throwIfExpired();
			if (!this.getStage().equals(Stage.REGISTERED)) {
				throw new InstallationException("The bundle is already installed.");
			}
			expire(Stage.INSTALLED);
			// null as InputStream lets the Framework guess how to access the URI instead of
			// simply reading the otherwise provided InputStream
			this.bundleRegistry.getBundleContext().installBundle(this.uri.toString(), null);
			final var installedBundle = new InstalledOsgiBundle(this);
			this.bundleRegistry.replaceBundleInstances(this, installedBundle);

			return installedBundle;
		} catch (final InstallationException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new InstallationException(exception);
		}
	}

	@Override
	public void unregister() throws UnregistrationException {
		try {
			throwIfExpired();
			if (!this.getStage().equals(Stage.REGISTERED)) {
				// TODO: automatically stop if already started, automatically uninstall if installed, update documentation
				throw new UnregistrationException(
						"The bundle must be stopped and uninstalled before it can be unregistered.");
			}

			expire(Stage.UNREGISTERED);
			this.bundleRegistry.remove(this);
		} catch (final UnregistrationException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new UnregistrationException(exception);
		}
	}

	@Override
	public Set<BundleConflict> getConflicts() {
		throwIfExpired();
		final String symbolicName = getSymbolicName();
		final var conflicts = new HashSet<BundleConflict>();
		for (final RegisteredBundle bundle : this.bundleRegistry.getBundles()) {
			if (this == bundle) {
				continue;
			}
			Set<String> conflictingClasses = getConflictingClassPaths(bundle);
			conflictingClasses = removeMatchingCrcContent(conflictingClasses, bundle);
			final boolean symbolicNameConflict = symbolicName.equals(bundle.getSymbolicName());
			if (!conflictingClasses.isEmpty() || symbolicNameConflict) {
				conflicts.add(new BundleConflict(bundle, conflictingClasses, symbolicNameConflict));
			}
		}

		return conflicts;
	}

	@Override
	public String getSymbolicNameWithVersion() {
		return getSymbolicName() + ':' + getVersion().toString();
	}

	void throwIfExpired() throws ExpiredException {
		if (isExpired()) {
			throw new ExpiredException(this.expiration.get());
		}
	}

	/**
	 * Removes entries from the given {@code classPaths} which have the same content
	 * CRC checksum in both bundles according to the JAR file.
	 * 
	 * @param classPaths
	 * @param bundle
	 * @return a new set not containing class paths to files with the same content
	 */
	protected Set<String> removeMatchingCrcContent(final Set<String> classPaths, final RegisteredBundle bundle) {
		return classPaths.stream().filter(classPath -> {
			final JarEntry thisEntry = this.classEntries.get(classPath);
			final JarEntry thatEntry = bundle.getClassEntries().get(classPath);

			return thisEntry.getCrc() != thatEntry.getCrc();
		}).collect(Collectors.toSet());
	}

	/**
	 * Determines and returns the class paths that are defined in both bundles
	 * (this instance and the given instance) and thus may cause conflicts.
	 * 
	 * @param bundle The bundle to compare this instance to.
	 * @return The conflicting class paths.
	 * @throws ExpiredException
	 */
	protected Set<String> getConflictingClassPaths(final RegisteredBundle bundle) throws ExpiredException {
		throwIfExpired();
		final var resultSet = new HashSet<>(this.classEntries.keySet());
		resultSet.retainAll(bundle.getClassEntries().keySet());

		return resultSet;
	}

	@SuppressWarnings({ "null", "static-method" })
	protected Map<String, JarEntry> determineClassEntries(final JarFile jar) {
		final var foundClassNames = new HashMap<String, JarEntry>();
		final Enumeration<@Nullable JarEntry> e = jar.entries();
		while (e.hasMoreElements()) {
			final JarEntry jarEntry = Checks.requireNonNull(e.nextElement());
			final String className = jarEntry.getName();
			if (className.endsWith(".class")) { //$NON-NLS-1$
				final @Nullable JarEntry previousJarEntry = foundClassNames.put(className, jarEntry);
				if (previousJarEntry != null) {
					throw new IllegalStateException(className);
				}
			}
		}
		return foundClassNames;
	}

	/**
	 * Inspects the JAR file backing the bundle for validity.
	 * <p>
	 * // TODO: get as much information as possible regarding the problem out
	 * 
	 * @throws BundleViolationException
	 */
	protected void throwIfInvalid() throws BundleViolationException {
		// TODO this is probably not complete and more attributes are mandatory, also
		// details like the correct version format could be checked too
		final List<Attributes.Name> mandatoryAttributes = Arrays.asList(SYMBOLIC_NAME, VERSION, BUNDLE_ACTIVATOR);

		for (final Attributes.Name mandatoryAttribute : mandatoryAttributes) {
			if (!this.manifest.getMainAttributes().containsKey(mandatoryAttribute)) {
				throw new BundleViolationException("Missing manifest key: " + mandatoryAttribute.toString());
			}
		}
	}

	@SuppressWarnings("null")
	protected void expire(final Stage newStage) {
		final Stage currentStage = getStage();
		this.expiration = Optional.of(new Expiration(currentStage, newStage));
	}

	/**
	 * The stage this instance is currently in. It can still be expired independent
	 * from the current stage.
	 * 
	 * @return The current stage.
	 */
	protected Stage getStage() {
		if (this instanceof StartedBundle) {
			return Stage.STARTED;
		}
		if (this instanceof InstalledBundle) {
			return Stage.INSTALLED;
		}

		return Stage.REGISTERED;
	}

	private static String getNameAttribute(final Attributes attributes, final Attributes.Name name) {
		return Checks.requireNonNull(attributes.getValue(name));
	}
}
