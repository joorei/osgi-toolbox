package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

import org.codeturnery.plugin.ExpiredException;
import org.codeturnery.plugin.InstalledPlugin;
import org.codeturnery.plugin.StartedPlugin;
import org.codeturnery.plugin.stage.Stage;
import org.codeturnery.plugin.stage.StartException;
import org.codeturnery.plugin.stage.UninstallationException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * The bundle is {@link Stage#REGISTERED} and was {@link Stage#INSTALLED} as
 * well. Thus, it may cause conflicts with other installed bundles (if not
 * expired) but will not affect the application with its functionality.
 * <p>
 * This intermediate stage between {@link Stage#REGISTERED} and
 * {@link Stage#STARTED} currently has no additional value over them. However,
 * it exists because it may be used in the future to allow bundles to depend on
 * the installation of other bundles without the dependency needed to be
 * started.
 */
class InstalledOsgiPlugin extends RegisteredOsgiPlugin implements InstalledPlugin {

	@SuppressWarnings("null")
	private final Instant installationTime = Instant.now();

	/**
	 * (Shallowly) copies the registration data from the given bundle to create a
	 * new instance with the current time as installation time.
	 * 
	 * @param bundle The newly created bundle, containing the registration data from
	 *               the given bundle.
	 */
	protected InstalledOsgiPlugin(final RegisteredOsgiPlugin bundle) {
		super(bundle);
	}

	@Override
	public Instant getInstallationTime() {
		return this.installationTime;
	}

	@Override
	public StartedPlugin start() throws StartException, ExpiredException {
		assertNotExpired();
		if (this instanceof StartedPlugin) {
			throw StartException.alreadyStarted(this, this.registry);
		}

		expire(Stage.STARTED);
		final StartedOsgiPlugin startedPlugin = new StartedOsgiPlugin(this);
		this.registry.replacePluginInstances(this, startedPlugin);
		try {
			getBundle().start();
			return startedPlugin;
		} catch (final BundleException | FrameworkException | BundleNotFoundException exception) {
			throw new StartException(exception, this, getRegistry());
		}
	}

	@Override
	public RegisteredOsgiPlugin uninstall() throws ExpiredException, UninstallationException {
		assertNotExpired();
		if (this instanceof StartedPlugin) {
			throw UninstallationException.stillStarted(this, getRegistry());
		}
		expire(Stage.REGISTERED);
		final RegisteredOsgiPlugin registeredOsgiPlugin = new RegisteredOsgiPlugin(this);
		this.registry.replacePluginInstances(this, registeredOsgiPlugin);
		try {
			getBundle().uninstall();
			return registeredOsgiPlugin;
		} catch (final BundleException | FrameworkException | BundleNotFoundException exception) {
			throw new UninstallationException(exception, this, getRegistry());
		}
	}

	@SuppressWarnings("null")
	Instant getLastModified() throws ExpiredException, FrameworkException, BundleNotFoundException {
		assertNotExpired();
		final long lastModifiedEpoch = getBundle().getLastModified();
		return Instant.ofEpochMilli(lastModifiedEpoch);
	}
	
	protected Bundle getBundle() throws FrameworkException, BundleNotFoundException {
		return this.registry.getBundleOrThrow(this.uri);
	}

}
