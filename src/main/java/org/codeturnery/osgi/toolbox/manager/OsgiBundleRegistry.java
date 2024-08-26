package org.codeturnery.osgi.toolbox.manager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import org.eclipse.jdt.annotation.Checks;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Wrapper around the backing OSGi framework implementation.
 * <p>
 * Implements {@link Closeable} for easier resource handling.
 * <p>
 * Should be used as singleton. Otherwise the detection for conflicting bundles
 * may get circumvented.
 */
// TODO: disallow concurrent access to writing methods
// TODO: expect bundles subtly but highly invalid and handle them when changing stages accordingly to not break registry state on exceptions
public class OsgiBundleRegistry extends AbstractBundleRegistry implements Closeable {

	protected final Framework framework;

	/**
	 * All known bundles of this registry, weather started, installed or just
	 * registered.
	 */
	protected final List<RegisteredOsgiBundle> bundles;

	/**
	 * @param extraExports Provide otherwise missing requirements to the bundles
	 *                     via the {@link Constants#FRAMEWORK_SYSTEMPACKAGES_EXTRA}
	 *                     option.
	 * @throws BundleException
	 *
	 * @see <a href=
	 *      "https://stackoverflow.com/questions/18303396/classcastexception-while-getting-the-service-that-has-been-registered-in-osgi">ClassCastException
	 *      while getting the service that has been registered in OSGi</a>
	 * @see <a href=
	 *      "https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-configuration-properties.html#_framework_configuration_properties">Apache
	 *      Felix Framework Configuration Properties</a>
	 */
	public OsgiBundleRegistry(final Set<String> extraExports) throws BundleException {
		final FrameworkFactory frameworkFactory = createFrameworkFactory();
		final Map<String, String> configuration = createFrameworkConfiguration(extraExports);
		this.framework = Checks.requireNonNull(frameworkFactory.newFramework(configuration));
		this.framework.start();
		this.bundles = new ArrayList<>();
	}

	@SuppressWarnings("null")
	@Override
	public List<RegisteredBundle> getBundles() {
		return Collections.unmodifiableList(this.bundles);
	}

	@Override
	public RegisteredBundle registerBundle(final File jarFile) throws RegistrationException {
		return new RegisteredOsgiBundle(jarFile, this);
	}

	@Override
	public void close() throws IOException {
		// TODO: lock this instance (and ideally all bundles) the moment the close method is called to prevent asynchronous changes
		
		// TODO: is this necessary? this potentially just claims services and
		// immediately releases them
		final var bundleContext = getBundleContext();
		final ServiceReference<?>[] references = this.framework.getRegisteredServices();
		for (final ServiceReference<?> reference : references) {
			bundleContext.ungetService(reference);
		}
		
		// TODO: stop/unistall/unregister all bundles propery
		
		// TODO: shouldn't the bundles be uninstalled before stopping the framework?
		// TODO: check if services are still in use before stopping?
		try {
			this.framework.stop();
		} catch (final BundleException exception) {
			throw new IOException(exception);
		}
	}

	@SuppressWarnings("null")
	@Override
	public <T> List<OsgiServiceWrapper<T>> loadServices(final Class<T> type) throws LoadServiceException {
		try {
			final BundleContext bundleContext = getBundleContext();
			final Collection<ServiceReference<T>> references = bundleContext.getServiceReferences(type, null);
			final var serviceWrappers = new ArrayList<OsgiServiceWrapper<T>>(references.size());
			for (final ServiceReference<T> reference : references) {
				serviceWrappers.add(createServiceWrapper(reference, type));
			}

			return serviceWrappers;
		} catch (final Throwable exception) {
			throw new LoadServiceException(type, exception);
		}
	}

	<T> boolean releaseService(final OsgiServiceWrapper<T> service) {
		return releaseService(service.getServiceReference());
	}

	<T> boolean releaseService(final ServiceReference<T> reference) {
		final BundleContext bundleContext = getBundleContext();
		return bundleContext.ungetService(reference);
	}

	Optional<RegisteredOsgiBundle> getBundleRegisteredFrom(final File jarFile) {
		for (final RegisteredOsgiBundle registeredBundle : this.bundles) {
			if (registeredBundle.isRegisteredFrom(jarFile)) {
				return Checks.requireNonNull(Optional.of(registeredBundle));
			}
		}

		return Checks.requireNonNull(Optional.empty());
	}

	BundleContext getBundleContext() throws IllegalArgumentException {
		switch (this.framework.getState()) {
		case Bundle.STOPPING:
		case Bundle.UNINSTALLED:
			throw new IllegalArgumentException();
		default:
			return Checks.requireNonNull(this.framework.getBundleContext());
		}
	}

	void replaceBundleInstances(final RegisteredOsgiBundle presentBundle, final RegisteredOsgiBundle replacement) {
		replacement.throwIfExpired();
		throwIfInRegistry(replacement);
		this.bundles.set(getBundleIndex(presentBundle), replacement);
	}

	void remove(final RegisteredOsgiBundle bundle) {
		final boolean wasPresent = this.bundles.remove(bundle);
		if (!wasPresent) {
			throw new UnknownBundleException(
					"The bundle can not be removed from registry as it is not present in it.");
		}
	}

	void add(final RegisteredOsgiBundle bundle) {
		bundle.throwIfExpired();
		throwIfInRegistry(bundle);
		this.bundles.add(bundle);
	}

	@SuppressWarnings("null")
	<T> Optional<T> loadBundleService(final ServiceReference<T> reference) {
		return Optional.ofNullable(getBundleContext().getService(reference));
	}

	@SuppressWarnings("null")
	Optional<Bundle> getBundle(final URI uri) {
		return Optional.ofNullable(getBundleContext().getBundle(Objects.requireNonNull(uri.toString())));
	}

	/**
	 * @return The implementation of a {@link FrameworkFactory} found by the
	 *         {@link ServiceLoader}.
	 * @throws NoSuchElementException
	 * @see <a href=
	 *      "https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-launching-and-embedding.html">apache-felix-framework-launching-and-embedding</a>
	 */
	@SuppressWarnings("static-method")
	protected FrameworkFactory createFrameworkFactory() throws NoSuchElementException {
		final ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader.load(FrameworkFactory.class);
		final Optional<FrameworkFactory> optionalframeworkFactory = serviceLoader.findFirst();
		return Checks.requireNonNull(optionalframeworkFactory.orElseThrow());
	}

	@SuppressWarnings("static-method")
	protected Map<String, String> createFrameworkConfiguration(final Set<String> extraExports) {
		final var configuration = new HashMap<String, String>();
		if (!extraExports.isEmpty()) {
			final String extras = Checks.requireNonNull(String.join(",", extraExports)); //$NON-NLS-1$
			configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extras);
		}
		/*
		 * Without this option a "felix-cache" directory will be automatically created
		 * that breaks the framework initialization on subsequent initializations.
		 */
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

		return configuration;
	}

	int getBundleIndex(final RegisteredOsgiBundle bundle) throws UnknownBundleException {
		final int index = this.bundles.indexOf(bundle);
		if (index < 0) {
			throw new UnknownBundleException("The accessed bundle is not known in this registry.");
		}

		return index;
	}

	protected void throwIfInRegistry(final RegisteredOsgiBundle bundle) {
		if (this.bundles.contains(bundle)) {
			throw new IllegalStateException("The bundle to add is already present in this registry.");
		}
	}

	protected <T> OsgiServiceWrapper<T> createServiceWrapper(final ServiceReference<T> reference,
			final Class<T> clazz) {
		return new OsgiServiceWrapper<>(reference, clazz, this);
	}
}
