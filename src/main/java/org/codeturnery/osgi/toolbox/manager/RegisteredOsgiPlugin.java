package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import org.codeturnery.plugin.AbstractRegisteredPlugin;
import org.codeturnery.plugin.InstalledPlugin;
import org.codeturnery.plugin.RegisteredPlugin;
import org.codeturnery.plugin.Registry;
import org.codeturnery.plugin.stage.Stage;
import org.codeturnery.plugin.StartedPlugin;
import org.codeturnery.plugin.version.StringVersion;
import org.codeturnery.plugin.version.Version;
import org.codeturnery.plugin.stage.AlreadyRegisteredException;
import org.codeturnery.plugin.ExpiredException;
import org.codeturnery.plugin.stage.InstallationException;
import org.codeturnery.plugin.stage.RegistrationException;
import org.codeturnery.plugin.stage.UnregistrationException;
import org.codeturnery.plugin.version.VersionFormatException;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleException;

class RegisteredOsgiPlugin extends AbstractRegisteredPlugin {
	private static final Attributes.Name VERSION = new Attributes.Name("Bundle-Version"); //$NON-NLS-1$
	private static final Attributes.Name SYMBOLIC_NAME = new Attributes.Name("Bundle-SymbolicName"); //$NON-NLS-1$
	private static final Attributes.Name BUNDLE_ACTIVATOR = new Attributes.Name("Bundle-Activator"); //$NON-NLS-1$
	/**
	 * The registry this bundle was registered into.
	 */
	protected final OsgiBundleRegistry registry;

	private final String symbolicName;
	private final Version version;

	RegisteredOsgiPlugin(final File jarFile, final OsgiBundleRegistry registry)
			throws IOException, RegistrationException {
		super(jarFile, registry);
		this.registry = registry;

		final Attributes attributes = Checks.requireNonNull(this.manifest.getMainAttributes());
		
		// retrieve symbolic name
		final @Nullable String symbolicName = attributes.getValue(SYMBOLIC_NAME);
		if (symbolicName == null) {
			throw new RegistrationException("Could not retrive symbolic name of plug-in via attribute " + SYMBOLIC_NAME, jarFile, registry);
		}
		this.symbolicName = symbolicName;
		
		// retrieve symbolic name
		final @Nullable String rawString = attributes.getValue(VERSION);
		if (rawString == null) {
			throw new RegistrationException("Could not retrive version of plug-in via attribute " + VERSION, jarFile, registry);
		}
		try {
			this.version = new StringVersion(rawString);
		} catch (final VersionFormatException exception) {
			throw new RegistrationException("Version format of plug-in is not supported: " + rawString, exception, jarFile, registry);
		}

		// check if JAR was already registered
		// TODO: check via checksum additionally or instead of path
		final Optional<RegisteredPlugin> existingPlugin = registry.getCorrespondingPlugin(jarFile);
		existingPlugin.ifPresent(plugin -> {
			throw new AlreadyRegisteredException(jarFile, plugin, registry);
		});

		// Inspect the JAR file for validity.
		// TODO this is probably not complete and more attributes are mandatory, also
		// details like the correct version format could be checked too
		final List<Attributes.Name> mandatoryAttributes = Arrays.asList(BUNDLE_ACTIVATOR);
		for (final Attributes.Name mandatoryAttribute : mandatoryAttributes) {
			if (!this.manifest.getMainAttributes().containsKey(mandatoryAttribute)) {
				throw new RegistrationException("Mandatory attribute missing: " + mandatoryAttribute, jarFile, registry);
			}
		}
		
		// FIXME: why can i directly access this method? do i want to?
		registry.addPlugin(this);
	}

	/**
	 * (Shallowly) copies the registration data from the given bundle to create a
	 * new instance that is (at least) registered.
	 * 
	 * @param pluginToCopy The newly created bundle, containing the registration data from
	 *               the given bundle.
	 */
	protected RegisteredOsgiPlugin(final RegisteredOsgiPlugin pluginToCopy) {
		super(pluginToCopy);
		this.registry = pluginToCopy.registry;
		this.version = pluginToCopy.version;
		this.symbolicName = pluginToCopy.symbolicName;
	}

	@Override
	public InstalledPlugin install() throws InstallationException, ExpiredException {
		assertNotExpired();
		if (this instanceof InstalledPlugin) {
			throw InstallationException.alreadyInstalled((InstalledPlugin) this, getRegistry());
		}
		expire(Stage.INSTALLED);
		final String uriString = this.uri.toString();
		try {
			// null as InputStream lets the Framework guess how to access the URI instead of
			// simply reading the otherwise provided InputStream
			this.registry.getBundleContext().installBundle(uriString, null);
		} catch (final BundleException | FrameworkException exception) {
			throw new InstallationException("Installing bundle from URI failed: " + uriString, exception, this, getRegistry());
		}
		final var installedPlugin = new InstalledOsgiPlugin(this);
		this.registry.replacePluginInstances(this, installedPlugin);

		return installedPlugin;
	}

	@Override
	public String getSymbolicName() throws ExpiredException {
		assertNotExpired();
		return this.symbolicName;
	}

	/**
	 * @return The version of this bundle.
	 */
	@Override
	public Version getVersion() throws ExpiredException {
		assertNotExpired();
		return this.version;
	}

	@Override
	public void unregister() throws UnregistrationException {
		assertNotExpired();
		if (this instanceof StartedPlugin) {
			throw UnregistrationException.notStopped(this, getRegistry());
		}
		if (this instanceof InstalledPlugin) {
			throw UnregistrationException.notUninstalled(this, getRegistry());
		}

		expire(Stage.UNREGISTERED);
		this.registry.removePlugin(this);
	}

	@Override
	protected Registry getRegistry() {
		return this.registry;
	}
}
