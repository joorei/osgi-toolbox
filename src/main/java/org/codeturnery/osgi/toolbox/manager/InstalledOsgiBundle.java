package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;
import org.eclipse.jdt.annotation.Checks;
import org.osgi.framework.Bundle;

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
class InstalledOsgiBundle extends RegisteredOsgiBundle implements InstalledBundle {

	private final Instant installationTime;

	protected InstalledOsgiBundle(final RegisteredOsgiBundle bundle) {
		super(bundle);
		this.installationTime = Checks.requireNonNull(Instant.now());
	}

	protected InstalledOsgiBundle(final InstalledOsgiBundle bundle) {
		super(bundle);
		this.installationTime = Checks.requireNonNull(Instant.now());
	}

	@Override
	public Instant getInstallationTime() {
		return this.installationTime;
	}

	@Override
	public StartedBundle start() throws StartException {
		try {
			throwIfExpired();
			if (!getStage().equals(Stage.INSTALLED)) {
				throw new StartException("The bundle must be registered and installed and not yet started to be started.");
			}

			expire(Stage.STARTED);
			final StartedOsgiBundle startedBundle = new StartedOsgiBundle(this);
			this.bundleRegistry.replaceBundleInstances(this, startedBundle);
			getBundle().start();

			return startedBundle;
		} catch (final StartException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new StartException(exception);
		}
	}

	@Override
	public RegisteredOsgiBundle uninstall() throws UninstallationException {
		try {
			throwIfExpired();
			if (!getStage().equals(Stage.INSTALLED)) {
				// TODO: automatically stop if already started, update documentation
				throw new UninstallationException("The bundle must be registered but stopped to be uninstalled.");
			}

			expire(Stage.REGISTERED);
			final RegisteredOsgiBundle registeredOsgiBundle = new RegisteredOsgiBundle(this);
			this.bundleRegistry.replaceBundleInstances(this, registeredOsgiBundle);
			getBundle().uninstall();

			return registeredOsgiBundle;
		} catch (final UninstallationException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new UninstallationException(exception);
		}
	}

	Instant getLastModified() {
		throwIfExpired();
		return Checks.requireNonNull(Instant.ofEpochMilli(getBundle().getLastModified()));
	}

	protected Bundle getBundle() {
		return Checks.requireNonNull(this.bundleRegistry.getBundleContext().getBundle(this.uri.toString()),
				"the bundle is installed and not expired but still no corresponding OSGi bundle was found");
	}
}
