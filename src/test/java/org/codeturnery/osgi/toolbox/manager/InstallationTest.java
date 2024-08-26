package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.Checks;
import org.junit.jupiter.api.Test;

public class InstallationTest extends BundleTest {

	@Test
	public void testInstallation() throws RegistrationException, InstallationException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerBundle(Checks.requireNonNull(file)).install();
		}
	}

	@Test
	public void testUninstallation() throws RegistrationException, InstallationException, UninstallationException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerBundle(Checks.requireNonNull(file)).install();
		}

		final var bundles = this.bundleRegistry.getBundles();

		final var bundleToBeUninstalled = (InstalledBundle) bundles.iterator().next();
		final var registeredBundle = bundleToBeUninstalled.uninstall();
		assertFalse(this.bundleRegistry.getBundles().contains(bundleToBeUninstalled));
		assertTrue(this.bundleRegistry.getBundles().contains(registeredBundle));
		assertTrue(bundleToBeUninstalled.isExpired());
		final Optional<Expiration> optionalExpiration = bundleToBeUninstalled.getExpiration();
		final Expiration expiration = Checks.requireNonNull(optionalExpiration.get());
		assertEquals(Stage.INSTALLED, expiration.getPreviousStage());
		assertEquals(Stage.REGISTERED, expiration.getNewStage());
	}
}
