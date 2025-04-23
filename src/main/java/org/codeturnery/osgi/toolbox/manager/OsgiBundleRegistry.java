package org.codeturnery.osgi.toolbox.manager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import org.codeturnery.plugin.AbstractRegistry;
import org.codeturnery.plugin.RegisteredPlugin;
import org.codeturnery.plugin.ExpiredException;
import org.codeturnery.plugin.CallServiceException;
import org.codeturnery.plugin.Plugin;
import org.codeturnery.plugin.stage.RegistrationException;
import org.codeturnery.plugin.RegistryException;
import org.eclipse.jdt.annotation.Checks;
import org.eclipse.jdt.annotation.Nullable;
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
 * Should be used as singleton. Otherwise the detection for conflicting plug-ins
 * may get circumvented.
 */
// TODO: disallow concurrent access to writing methods
// TODO: expect bundles subtly but highly invalid and handle them when changing stages accordingly to not break registry state on exceptions
public class OsgiBundleRegistry extends AbstractRegistry implements Closeable {

	protected final Framework framework;

	/**
	 * @param extraExports Provide otherwise missing requirements to the bundles via
	 *                     the {@link Constants#FRAMEWORK_SYSTEMPACKAGES_EXTRA}
	 *                     option.
	 * @throws BundleException If the underlying OSGi framework could not be started.
	 * @throws NoSuchElementException If no service (i.e. implementation was found
	 *                                corresponding to the {@link Framework}
	 *                                interface.
	 *
	 * @see <a href=
	 *      "https://stackoverflow.com/questions/18303396/classcastexception-while-getting-the-service-that-has-been-registered-in-osgi">ClassCastException
	 *      while getting the service that has been registered in OSGi</a>
	 * @see <a href=
	 *      "https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-configuration-properties.html#_framework_configuration_properties">Apache
	 *      Felix Framework Configuration Properties</a>
	 */
	public OsgiBundleRegistry(final Set<String> extraExports) throws BundleException, NoSuchElementException {
		super();
		final FrameworkFactory frameworkFactory = createFrameworkFactory();
		final Map<String, String> configuration = createFrameworkConfiguration(extraExports);
		this.framework = Checks.requireNonNull(frameworkFactory.newFramework(configuration));
		this.framework.start();
	}

	@Override
	public RegisteredPlugin registerPlugin(final File jarFile) throws RegistrationException, IOException {
		return new RegisteredOsgiPlugin(jarFile, this);
	}

	@Override
	public void close() throws IOException {
		// TODO: lock this instance (and ideally all plug-ins) the moment the close
		// method is called to prevent asynchronous changes

		try {
			final var bundleContext = getBundleContext();

			// TODO: is this necessary? this potentially just claims services and
			// immediately releases them
			final ServiceReference<?>[] references = this.framework.getRegisteredServices();
			for (final ServiceReference<?> reference : references) {
				bundleContext.ungetService(reference);
			}
		} catch (final FrameworkException frameworkException) {
			throw new IOException(frameworkException);
		}

		// TODO: should the plug-ins be (automatically) removed before stopping the framework or not?
		forceUnregisterAllPlugins();

		// TODO: check if services are still in use before stopping?
		try {
			this.framework.stop();
		} catch (final BundleException exception) {
			throw new IOException(exception);
		}
	}

	@SuppressWarnings("null")
	@Override
	public <T> List<OsgiServiceWrapper<T>> loadServices(final Class<T> type) throws CallServiceException {
		try {
			final BundleContext bundleContext = getBundleContext();
			final Collection<ServiceReference<T>> references = bundleContext.getServiceReferences(type, null);
			final var serviceWrappers = new ArrayList<OsgiServiceWrapper<T>>(references.size());
			for (final ServiceReference<T> reference : references) {
				serviceWrappers.add(createServiceWrapper(reference, type));
			}

			return serviceWrappers;
		} catch (final Throwable exception) {
			throw new CallServiceException(exception, type, this);
		}
	}

	<T> boolean releaseService(final OsgiServiceWrapper<T> service) throws FrameworkException {
		return releaseService(service.getServiceReference());
	}

	<T> boolean releaseService(final ServiceReference<T> reference) throws FrameworkException {
		final BundleContext bundleContext = getBundleContext();
		return bundleContext.ungetService(reference);
	}

	/**
	 * @return the {@link BundleContext} of the {@link Framework} bundle.
	 * @throws FrameworkException if access failed for some reason
	 */
	BundleContext getBundleContext() throws FrameworkException {
		final int frameworkState = this.framework.getState();
		switch (frameworkState) {
		case Bundle.STOPPING:
		case Bundle.UNINSTALLED:
			throw new FrameworkStateException(frameworkState);
		default:
			final @Nullable BundleContext bundleContext = this.framework.getBundleContext();
			if (bundleContext == null) {
				throw new FrameworkBundleContextNullException(frameworkState);
			}
			return bundleContext;
		}
	}

	@SuppressWarnings("null")
	<T> Optional<T> loadBundleService(final ServiceReference<T> reference) throws FrameworkException {
		return Optional.ofNullable(getBundleContext().getService(reference));
	}

	Bundle getBundleOrThrow(final URI uri) throws FrameworkException, BundleNotFoundException {
		final String uriString = Objects.requireNonNull(uri.toString());
		final @Nullable Bundle bundle = getBundleContext().getBundle(uriString);
		if (bundle == null) {
			throw new BundleNotFoundException("No bundle found for the URI " + uriString);
		}
		return bundle;
	}

	/**
	 * @return The implementation of a {@link FrameworkFactory} found by the
	 *         {@link ServiceLoader}.
	 * @throws NoSuchElementException If no service was found corresponding to the
	 *                                {@link Framework} interface.
	 * @see <a href=
	 *      "https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-launching-and-embedding.html">apache-felix-framework-launching-and-embedding</a>
	 */
	@SuppressWarnings("static-method")
	protected FrameworkFactory createFrameworkFactory() throws NoSuchElementException {
		final ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader.load(FrameworkFactory.class);
		final Optional<FrameworkFactory> optionalframeworkFactory = serviceLoader.findFirst();

		return optionalframeworkFactory.orElseThrow();
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

	protected <T> OsgiServiceWrapper<T> createServiceWrapper(final ServiceReference<T> reference,
			final Class<T> clazz) {
		return new OsgiServiceWrapper<>(reference, clazz, this);
	}

	@Override
	protected void replacePluginInstances(final Plugin presentPlugin, final Plugin replacementPlugin)
			throws ExpiredException, RegistryException {
		super.replacePluginInstances(presentPlugin, replacementPlugin);
	}
	
	@Override
	protected Optional<RegisteredPlugin> getCorrespondingPlugin(File jarFile) {
		return super.getCorrespondingPlugin(jarFile);
	}
	
	@Override
	protected void addPlugin(final Plugin plugin) throws RegistryException {
		super.addPlugin(plugin);
	}
	
	@Override
	protected void removePlugin(final Plugin plugin) throws RegistryException {
		super.removePlugin(plugin);
	}
}
