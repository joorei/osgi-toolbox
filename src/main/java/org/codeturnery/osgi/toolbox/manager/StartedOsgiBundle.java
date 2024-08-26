package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

import org.eclipse.jdt.annotation.Checks;

/**
 * The bundle is registered, installed and was started. Thus, it may cause
 * conflicts with other installed bundles and affects the behavior of the
 * application.
 */
class StartedOsgiBundle extends InstalledOsgiBundle implements StartedBundle {

	private final Instant startTime;
	
	protected StartedOsgiBundle(final InstalledOsgiBundle bundle) {
		super(bundle);
		this.startTime = Checks.requireNonNull(Instant.now());
	}

	@Override
	public Instant getStartTime() {
		return this.startTime;
	}

	@Override
	public InstalledBundle stop() throws StopException {
		try {
			throwIfExpired();
			if (!getStage().equals(Stage.STARTED)) {
				throw new StopException("The bundle must be started to be stopped.");
			}

			expire(Stage.INSTALLED);
			final InstalledOsgiBundle installedOsgiBundle = new InstalledOsgiBundle(this);
			this.bundleRegistry.replaceBundleInstances(this, installedOsgiBundle);
			getBundle().stop();

			return installedOsgiBundle;
		} catch (final StopException exception) {
			throw exception;
		} catch (final Throwable exception) {
			throw new StopException(exception);
		}
	}
}
