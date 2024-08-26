package org.codeturnery.osgi.toolbox.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;
import org.codeturnery.proxies.LockedException;

public class ServiceReleaseTest extends BundleTest {

	/**
	 * This is not an actual test but shows how the reference counting of OSGi
	 * works.
	 * 
	 * @throws InvalidSyntaxException
	 * @throws StageChangeException
	 */
	@Test
	public void testReleaseCounter() throws InvalidSyntaxException, StageChangeException {
		final var startedBundle = this.bundleRegistry.registerBundle(getBundleJarFiles().get(0)).install().start();
		final var bundleContext = this.bundleRegistry.getBundleContext();

		final var refsA = new ArrayList<>(bundleContext.getServiceReferences(BookImporter.class, null));
		final var refsB = new ArrayList<>(bundleContext.getServiceReferences(BookImporter.class, null));
		assertNotSame(refsA, refsB);
		assertEquals(refsA, refsB);
		assertEquals(1, refsA.size());
		assertEquals(1, refsB.size());
		assertSame(refsA.get(0), refsB.get(0));
		final var ref = refsA.get(0);

		var servicesInUse = bundleContext.getBundle().getServicesInUse();
		assertNull(servicesInUse);

		final var bundle = this.bundleRegistry.getBundle(startedBundle.getUri()).get();
		assertNull(bundle.getServicesInUse());

		final var serviceA = bundleContext.getService(ref);
		servicesInUse = bundleContext.getBundle().getServicesInUse();
		assertEquals(1, servicesInUse.length);
		assertNull(bundle.getServicesInUse());

		final var serviceB = bundleContext.getService(ref);
		servicesInUse = bundleContext.getBundle().getServicesInUse();
		assertEquals(1, servicesInUse.length);
		assertNull(bundle.getServicesInUse());

		assertSame(serviceA, serviceB);

		// we fetched the same service two times, so we can release it two times
		assertTrue(bundleContext.ungetService(ref));
		servicesInUse = bundleContext.getBundle().getServicesInUse();
		assertEquals(1, servicesInUse.length);
		assertNull(bundle.getServicesInUse());

		assertTrue(bundleContext.ungetService(ref));
		servicesInUse = bundleContext.getBundle().getServicesInUse();
		assertNull(servicesInUse);
		assertNull(bundle.getServicesInUse());

		assertFalse(bundleContext.ungetService(ref));
	}

	@Test
	public void testRelease() throws StageChangeException, LoadServiceException {
		this.bundleRegistry.registerBundle(getBundleJarFiles().get(0)).install().start();

		@Nullable
		OsgiServiceWrapper<BookImporter> service = getServiceWrapper();
		assertFalse(this.bundleRegistry.releaseService(service));
		service = null;

		final var serviceWrapperA = getServiceWrapper();
		final var serviceA = serviceWrapperA.getService();
		final var serviceWrapperB = getServiceWrapper();
		final var serviceB = serviceWrapperB.getService();
		
		assertSame(serviceA, serviceB);
		assertTrue(this.bundleRegistry.releaseService(serviceWrapperA));
		assertTrue(this.bundleRegistry.releaseService(serviceWrapperB));
		assertFalse(this.bundleRegistry.releaseService(serviceWrapperA));
	}

	@Test
	public void testReleaseMultipleServices()
			throws StageChangeException, LoadServiceException {
		for (final var bundleJarFile : getBundleJarFiles()) {
			this.bundleRegistry.registerBundle(bundleJarFile).install().start();
		}

		var serviceProxies = this.bundleRegistry.loadServices(BookImporter.class);
		assertEquals(4, serviceProxies.size());
		final var undeclaredCatches = new ArrayList<UndeclaredThrowableException>();
		final var methodCatches = new ArrayList<AbstractMethodError>();
		for (final var serviceProxy : serviceProxies) {
			final var extractedServices = new ArrayList<BookImporter>(1);
			serviceProxy.callService(s -> {
				try {
					s.getBookCount();
				} catch (final UndeclaredThrowableException undeclaredThrowableException) {
					// the method to call didn't exist on the bundle service itself
					undeclaredCatches.add(undeclaredThrowableException);
				}
				final var books = s.getBooks();
				try {
					books.iterator().next().getAuthorName();
				} catch (final AbstractMethodError abstractMethodError) {
					// the method to call didn't exist in some implementation of the bundle
					methodCatches.add(abstractMethodError);
				}
				extractedServices.add(s);
			});
			assertEquals(1, extractedServices.size());
			assertThrows(LockedException.class, () -> extractedServices.get(0).getBooks().iterator().next().getTitle());
			assertNotSame(extractedServices.get(0), serviceProxy.getService());
			assertTrue(this.bundleRegistry.releaseService(serviceProxy));
			assertFalse(this.bundleRegistry.releaseService(serviceProxy));
		}
		assertEquals(1, undeclaredCatches.size());
		assertEquals(1, methodCatches.size());
		final InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class,
				() -> {
					throw undeclaredCatches.get(0).getCause();
				});
		final AbstractMethodError abstractMethodError = assertThrows(AbstractMethodError.class, () -> {
			throw invocationTargetException.getCause();
		});
		assertNull(abstractMethodError.getCause());
	}

	private OsgiServiceWrapper<BookImporter> getServiceWrapper() throws LoadServiceException {
		final var services = this.bundleRegistry.loadServices(BookImporter.class);
		assertEquals(1, services.size());
		return services.get(0);
	}
}
