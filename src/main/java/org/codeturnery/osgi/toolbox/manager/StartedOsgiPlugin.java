package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

import org.codeturnery.plugin.InstalledPlugin;
import org.codeturnery.plugin.stage.Stage;
import org.codeturnery.plugin.StartedPlugin;
import org.codeturnery.plugin.stage.StopException;
import org.osgi.framework.BundleException;

/**
 * The bundle is registered, installed and was started. Thus, it may cause
 * conflicts with other installed bundles and affects the behavior of the
 * application.
 */
class StartedOsgiPlugin extends InstalledOsgiPlugin implements StartedPlugin {

	@SuppressWarnings("null")
	private final Instant startTime = Instant.now();
	
	protected StartedOsgiPlugin(final InstalledOsgiPlugin plugin) {
		super(plugin);
	}

	@Override
	public Instant getStartTime() {
		return this.startTime;
	}

	@Override
	public InstalledPlugin stop() throws StopException {
		assertNotExpired();
		expire(Stage.INSTALLED);
		final InstalledOsgiPlugin installedOsgiPlugin = new InstalledOsgiPlugin(this);
		this.registry.replacePluginInstances(this, installedOsgiPlugin);
		try {
			getBundle().stop();
			return installedOsgiPlugin;
		} catch (final BundleException | FrameworkException | BundleNotFoundException exception) {
			throw new StopException("Failed to stop plug-in.", exception, this, getRegistry());
		}
	}
}
