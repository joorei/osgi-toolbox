package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.codeturnery.osgi.fixtures.bundles.contract.Book;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class  was intended to demonstrate how the install and start order affects bundles with conflicts.
 * The behavior is not transparent. Previously it was possible to load two conflicting bundles with them both
 * returning the same service implementation even though it should be different. However, it is no longer
 * possible to reproduce that. Even though the test installs and starts bundles in different orders, in all
 * cases having the same bundles in different versions active is no problem. Each one returns the correct
 * corresponding service implementation.
 * <p>
 * Previously it was possible to trigger the runtime usage of the old implementation  in
 * {@link StartAndLoadOrderTest#testServicesEachInstalledAndStartedReversed}.
 * <p>
 * It was assumed that maybe OSGi automatically prefers the newest version in case of conflicts when
 * the bundle is started, because the mentioned test is the only one that would
 * force OSGi to start the old bundle before the newer one is even known.
 */
public class StartAndLoadOrderTest extends BundleTest {

	@Test
	public void testServicesEachInstalledAndStarted()
			throws StageChangeException, LoadServiceException {
		assertNotNull(this.bundleRegistry);
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerBundle(file).install().start();
		}

		checkBundles();
	}

	@Test
	public void testServicesEachInstalledAndStartedReversed()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		Collections.reverse(files);
		for (final File file : files) {
			this.bundleRegistry.registerBundle(file).install().start();
		}


		checkBundles();
	}

	@Test
	public void testServicesAllInstalledUnreversedAndThenAllStartedUnreversed()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		final var installedBundles = new ArrayList<InstalledBundle>(files.size());
		for (final File file : files) {
			installedBundles.add(this.bundleRegistry.registerBundle(file).install());
		}
		for (final InstalledBundle installedBundle : installedBundles) {
			installedBundle.start();
		}

		checkBundles();
	}

	@Test
	public void testServicesAllInstalledReversedAndThenStartUnreversed()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		Collections.reverse(files);
		final var installedBundles = new ArrayList<InstalledBundle>(files.size());
		for (final File file : files) {
			installedBundles.add(this.bundleRegistry.registerBundle(file).install());
		}
		Collections.reverse(installedBundles);
		for (final InstalledBundle installedBundle : installedBundles) {
			installedBundle.start();
		}

		checkBundles();
	}

	@Test
	public void testServicesAllInstalledReversedAndThenStartReversed()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		Collections.reverse(files);
		final var installedBundles = new ArrayList<InstalledBundle>(files.size());
		for (final File file : files) {
			installedBundles.add(this.bundleRegistry.registerBundle(file).install());
		}
		for (final InstalledBundle installedBundle : installedBundles) {
			installedBundle.start();
		}

		checkBundles();
	}

	@Test
	public void testUnregisteringStartedServices()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		for (final File file : files) {
			this.bundleRegistry.registerBundle(file).install().start();
		}


		checkBundles();
		
		new ArrayList<>(this.bundleRegistry.getBundles()).stream().filter(this.bundleRegistry::isStarted).map(StartedBundle.class::cast).forEach(bundle -> {
			try {
				bundle.stop().uninstall().unregister();
			} catch (UnregistrationException | UninstallationException | StopException e) {
				throw new RuntimeException(e);
			}
		});
		

		final List<OsgiServiceWrapper<BookImporter>> nameProviders = this.bundleRegistry
				.loadServices(BookImporter.class);

		assertEquals(0, nameProviders.size());
	}

	@Test
	public void testServicesAllInstalledUnreversedAndThenStartReversed()
			throws StageChangeException, LoadServiceException {
		final List<File> files = getBundleJarFiles();
		final var installedBundles = new ArrayList<InstalledBundle>(files.size());
		for (final File file : files) {
			installedBundles.add(this.bundleRegistry.registerBundle(file).install());
		}
		Collections.reverse(installedBundles);
		for (final InstalledBundle installedBundle : installedBundles) {
			installedBundle.start();
		}

		checkBundles();
	}
	
	private void checkBundles() {
		final List<OsgiServiceWrapper<BookImporter>> nameProviders = this.bundleRegistry
				.loadServices(BookImporter.class);
		Set<String> titles = new HashSet<>();
		
		// the services are returned in different order over multiple test executions, so we loop over them
		for (final var service : nameProviders) {
			final String title = getTitle(service);
			switch (title) {
			case "It":
				checkReturn(service, title, null, true);
				titles.add(title);
				break;
			case "T: The Dark Tower":
				checkReturn(service, "T: The Dark Tower", "A: King", false);
				titles.add(title);
				break;
			case "Bag of Bones":
				checkReturn(service, "Bag of Bones", "Steven King", false);
				titles.add(title);
				break;
			case "Title: The Dark Tower":
				checkReturn(service, "Title: The Dark Tower", "Author: King", false);
				titles.add(title);
				break;
			default:
				throw new RuntimeException();
			}
		}

		assertEquals(4, titles.size());
	}

	private static String getTitle(OsgiServiceWrapper<BookImporter> osgiServiceWrapper) {
		return osgiServiceWrapper.getService().getBooks().iterator().next().getTitle();
	}
	
	private static void checkReturn(OsgiServiceWrapper<BookImporter> osgiServiceWrapper, String title, @Nullable String author, boolean expectedCountCatch) {
		final var service = osgiServiceWrapper.getService();

		boolean reachedCatch = false;
		
		final Book book = service.getBooks().iterator().next();
		assertEquals(title, book.getTitle());
		
		
		try {
			final String actualAuthor = service.getBooks().iterator().next().getAuthorName();
			assertEquals(author, actualAuthor);
		} catch (@SuppressWarnings("unused") final AbstractMethodError e) {
			// handle method potentially not present in old bundles

			boolean foundAuthorMethod = false;
			boolean foundTitleMethod = false;
			
			// we must not use getMethods here, as it would return methods defined in the parent classes as well
			for (final Method method : book.getClass().getDeclaredMethods()) {
				if ("getAuthorName".equals(method.getName())) {
					foundAuthorMethod = true;
				}
				if ("getTitle".equals(method.getName())) {
					foundTitleMethod = true;
				}
			}
			assertFalse(foundAuthorMethod);
			assertTrue(foundTitleMethod);
		}
		
		try {
			service.getBookCount();
		} catch (@SuppressWarnings("unused") final AbstractMethodError e) {
			// handle method potentially not present in old bundles
			
			reachedCatch = true;

			boolean foundBooksMethod = false;
			boolean foundCountMethod = false;
			// we must not use getMethods here, as it would return methods defined in the parent classes as well
			for (final Method method : service.getClass().getDeclaredMethods()) {
				if ("getBookCount".equals(method.getName())) {
					foundCountMethod = true;
				}
				if ("getBooks".equals(method.getName())) {
					foundBooksMethod = true;
				}
			}
			assertFalse(foundCountMethod);
			assertTrue(foundBooksMethod);
		}
		
		assertTrue(expectedCountCatch == reachedCatch);
	}
}
