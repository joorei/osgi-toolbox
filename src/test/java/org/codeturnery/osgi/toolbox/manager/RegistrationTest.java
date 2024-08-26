package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Checks;
import org.junit.jupiter.api.Test;

public class RegistrationTest extends BundleTest {

	@Test
	public void testRegistration() throws RegistrationException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			final var bundle = this.bundleRegistry.registerBundle(Checks.requireNonNull(file));
			assertTrue(this.bundleRegistry.getBundles().contains(bundle));
			assertEquals(bundle, this.bundleRegistry.getBundleRegisteredFrom(Checks.requireNonNull(file)).get());
		}
		final Map<String, RegisteredBundle> bundleMap = getBundleMap(files.size());

		final RegisteredBundle a = bundleMap.get(A.getIdentifier());
		final RegisteredBundle b = bundleMap.get(B.getIdentifier());
		final RegisteredBundle c1 = bundleMap.get(C1.getIdentifier());
		final RegisteredBundle c2 = bundleMap.get(C2.getIdentifier());

		assertTrue(a.getConflicts().isEmpty());
		assertTrue(b.getConflicts().isEmpty());

		// We expect the bundle c to conflict with its different versions as they have
		// the same symbolic name and conflict in the BookImpl.class because that one
		// has changed between the two versions.
		assertTrue(c1.getConflicts().equals(Set.of(new BundleConflict(c2, C_CONFLICTS, true))));
		assertTrue(c2.getConflicts().equals(Set.of(new BundleConflict(c1, C_CONFLICTS, true))));
	}

	@Test
	public void testUnregistration() throws RegistrationException, UnregistrationException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerBundle(file);
		}

		var bundleMap = getBundleMap(files.size());

		final RegisteredBundle a = bundleMap.get(A.getIdentifier());
		final RegisteredBundle b = bundleMap.get(B.getIdentifier());
		final RegisteredBundle c1 = bundleMap.get(C1.getIdentifier());
		final RegisteredBundle c2 = bundleMap.get(C2.getIdentifier());

		final RegisteredBundle bundleToBeUnregistered = c2;
		bundleToBeUnregistered.unregister();
		List<RegisteredBundle> bundles = this.bundleRegistry.getBundles();
		assertFalse(bundles.contains(bundleToBeUnregistered));
		assertEquals(3, bundles.size());
		assertTrue(bundleToBeUnregistered.isExpired());
		final var expiration = bundleToBeUnregistered.getExpiration().get();
		assertEquals(Stage.REGISTERED, expiration.getPreviousStage());
		assertEquals(Stage.UNREGISTERED, expiration.getNewStage());

		assertTrue(a.getConflicts().isEmpty());
		assertTrue(b.getConflicts().isEmpty());
		// after unregistering one of the two versions of the same bundle there are no
		// conflicts left
		assertTrue(c1.getConflicts().isEmpty());
	}

	protected Map<String, RegisteredBundle> getBundleMap(int size) {
		final List<RegisteredBundle> bundles = this.bundleRegistry.getBundles();
		final int expectedSize = getBundleJarFiles().size();
		assertEquals(size, bundles.size());
		assertEquals(expectedSize, size);
		final Map<String, RegisteredBundle> bundleMap = new HashMap<>(expectedSize);
		for (final RegisteredBundle bundle : bundles) {
			bundleMap.put(bundle.getSymbolicNameWithVersion(), bundle);
		}
		assertEquals(expectedSize, bundleMap.size());

		return bundleMap;
	}
}
