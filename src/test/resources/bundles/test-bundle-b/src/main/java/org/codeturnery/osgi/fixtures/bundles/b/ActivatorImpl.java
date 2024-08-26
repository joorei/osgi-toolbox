package org.codeturnery.osgi.fixtures.bundles.b;

import java.util.Hashtable;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.codeturnery.osgi.fixtures.bundles.b.importer.BookImporterImpl;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;

public class ActivatorImpl implements BundleActivator {
	private @Nullable ServiceRegistration<BookImporter> registration;

	@Override
	public void start(final @Nullable BundleContext context) {
		final var config = new Hashtable<String, String>();
		final var bookImporter = new BookImporterImpl();
		this.registration = context.registerService(BookImporter.class, bookImporter, config);
	}

	@Override
	public void stop(final @Nullable BundleContext context) {
		if (this.registration != null) {
			this.registration.unregister();
		}
	}
}
