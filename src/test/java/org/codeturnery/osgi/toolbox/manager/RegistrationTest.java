package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codeturnery.plugin.Conflict;
import org.codeturnery.plugin.RegisteredPlugin;
import org.codeturnery.plugin.stage.Stage;
import org.codeturnery.plugin.stage.RegistrationException;
import org.codeturnery.plugin.stage.UnregistrationException;
import org.eclipse.jdt.annotation.Checks;
import org.junit.jupiter.api.Test;

public class RegistrationTest extends BundleTest {

	@Test
	public void testRegistration() throws RegistrationException, IOException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			final var bundle = this.bundleRegistry.registerPlugin(Checks.requireNonNull(file));
			assertTrue(this.bundleRegistry.getPlugins().contains(bundle));
			assertEquals(bundle, this.bundleRegistry.getCorrespondingPlugin(Checks.requireNonNull(file)).get());
		}
		final Map<String, RegisteredPlugin> bundleMap = getBundleMap(files.size());

		final RegisteredPlugin a = bundleMap.get(A.getIdentifier());
		final RegisteredPlugin b = bundleMap.get(B.getIdentifier());
		final RegisteredPlugin c1 = bundleMap.get(C1.getIdentifier());
		final RegisteredPlugin c2 = bundleMap.get(C2.getIdentifier());

		assertTrue(a.getConflicts().isEmpty());
		assertTrue(b.getConflicts().isEmpty());

		// We expect the bundle c to conflict with its different versions as they have
		// the same symbolic name and conflict in the BookImpl.class because that one
		// has changed between the two versions.
		assertTrue(c1.getConflicts().equals(Set.of(new Conflict(c2, C_CONFLICTS, true))));
		assertTrue(c2.getConflicts().equals(Set.of(new Conflict(c1, C_CONFLICTS, true))));
	}

	@Test
	public void testUnregistration() throws RegistrationException, UnregistrationException, IOException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerPlugin(file);
		}

		var bundleMap = getBundleMap(files.size());

		final RegisteredPlugin a = bundleMap.get(A.getIdentifier());
		final RegisteredPlugin b = bundleMap.get(B.getIdentifier());
		final RegisteredPlugin c1 = bundleMap.get(C1.getIdentifier());
		final RegisteredPlugin c2 = bundleMap.get(C2.getIdentifier());

		final RegisteredPlugin pluginToBeUnregistered = c2;
		pluginToBeUnregistered.unregister();
		Set<RegisteredPlugin> plugins = this.bundleRegistry.getRegisteredPlugins();
		assertFalse(plugins.contains(pluginToBeUnregistered));
		assertEquals(3, plugins.size());
		assertTrue(pluginToBeUnregistered.isExpired());
		final var expiration = pluginToBeUnregistered.getExpiration().get();
		assertEquals(Stage.REGISTERED, expiration.getPreviousStage());
		assertEquals(Stage.UNREGISTERED, expiration.getNewStage());

		assertTrue(a.getConflicts().isEmpty());
		assertTrue(b.getConflicts().isEmpty());
		// after unregistering one of the two versions of the same bundle there are no
		// conflicts left
		assertTrue(c1.getConflicts().isEmpty());
	}

	protected Map<String, RegisteredPlugin> getBundleMap(int size) {
		final Set<RegisteredPlugin> plugins = this.bundleRegistry.getRegisteredPlugins();
		final int expectedSize = getBundleJarFiles().size();
		assertEquals(size, plugins.size());
		assertEquals(expectedSize, size);
		final Map<String, RegisteredPlugin> bundleMap = new HashMap<>(expectedSize);
		for (final RegisteredPlugin plugin : plugins) {
			bundleMap.put(plugin.getSymbolicNameWithVersion(), plugin);
		}
		assertEquals(expectedSize, bundleMap.size());

		return bundleMap;
	}
}
