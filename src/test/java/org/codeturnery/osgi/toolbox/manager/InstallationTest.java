package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.codeturnery.plugin.Expiration;
import org.codeturnery.plugin.InstalledPlugin;
import org.codeturnery.plugin.stage.Stage;
import org.codeturnery.plugin.stage.InstallationException;
import org.codeturnery.plugin.stage.RegistrationException;
import org.codeturnery.plugin.stage.UninstallationException;
import org.eclipse.jdt.annotation.Checks;
import org.junit.jupiter.api.Test;

public class InstallationTest extends BundleTest {

	@Test
	public void testInstallation() throws RegistrationException, InstallationException, IOException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerPlugin(Checks.requireNonNull(file)).install();
		}
	}

	@Test
	public void testUninstallation() throws RegistrationException, InstallationException, UninstallationException, IOException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerPlugin(Checks.requireNonNull(file)).install();
		}

		final var bundles = this.bundleRegistry.getPlugins();

		final var bundleToBeUninstalled = (InstalledPlugin) bundles.iterator().next();
		final var registeredBundle = bundleToBeUninstalled.uninstall();
		assertFalse(this.bundleRegistry.getPlugins().contains(bundleToBeUninstalled));
		assertTrue(this.bundleRegistry.getPlugins().contains(registeredBundle));
		assertTrue(bundleToBeUninstalled.isExpired());
		final Optional<Expiration> optionalExpiration = bundleToBeUninstalled.getExpiration();
		final Expiration expiration = Checks.requireNonNull(optionalExpiration.get());
		assertEquals(Stage.INSTALLED, expiration.getPreviousStage());
		assertEquals(Stage.REGISTERED, expiration.getNewStage());
	}
}
