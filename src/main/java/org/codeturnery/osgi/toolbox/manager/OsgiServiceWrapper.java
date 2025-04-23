package org.codeturnery.osgi.toolbox.manager;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Checks;
import org.osgi.framework.ServiceReference;
import org.codeturnery.plugin.ServiceWrapper;
import org.codeturnery.plugin.CallServiceException;
import org.codeturnery.proxies.LockableProxyFactory;

/**
 * This wrapper instance hides the actual service from the calling application.
 * This is necessary because references to service objects must be handled
 * carefully, to allow the garbage collector to clean them up.
 * <p>
 * When invoking the {@link #callService(Function)} method in this instance you
 * will be passed the actual service into your function.
 * <p>
 * <strong>Do not keep the reference to the service by saving it into your own
 * objects.</strong> You will get exceptions when calling its methods after your
 * {@link Consumer} function returned.
 *
 * @param <T> The type of service wrapped by this instance.
 */
class OsgiServiceWrapper<T> implements ServiceWrapper<T> {
	private final ServiceReference<T> reference;
	private final OsgiBundleRegistry registry;
	private final Class<T> clazz;

	OsgiServiceWrapper(final ServiceReference<T> reference, final Class<T> clazz,
			final OsgiBundleRegistry bundleRegistry) {
		this.reference = Checks.requireNonNull(reference);
		this.clazz = Checks.requireNonNull(clazz);
		this.registry = Checks.requireNonNull(bundleRegistry);
	}

	@Override
	public synchronized <R> R callService(final Function<T, R> function) throws CallServiceException {
		try {
			final T targetService = getService();
			final LockableProxyFactory<T> proxyFactory = new LockableProxyFactory<>(targetService, this.clazz);
			try {
				return function.apply(proxyFactory.createProxy());
			} finally {
				proxyFactory.lock();
				this.registry.releaseService(this.reference);
			}
		} catch (final FrameworkException frameworkException) {
			throw new CallServiceException("Failed to load or release service.", frameworkException, this.clazz, this.registry);
		}
	}
	
	@Override
	public synchronized void callService(final Consumer<T> consumer) throws CallServiceException {
		try {
			final T targetService = getService();
			final LockableProxyFactory<T> proxyFactory = new LockableProxyFactory<>(targetService, this.clazz);
			try {
				consumer.accept(proxyFactory.createProxy());
			} finally {
				proxyFactory.lock();
				this.registry.releaseService(this.reference);
			}
		} catch (final FrameworkException frameworkException) {
			throw new CallServiceException("Failed to load or release service.", frameworkException, this.clazz, this.registry);
		}
	}

	T getService() throws CallServiceException, FrameworkException {
		final Optional<T> optionalService = this.registry.loadBundleService(this.reference);
		
		return optionalService.orElseThrow(() -> 
			new ServiceReferenceException(this.reference, this.clazz, this.registry)
		);
	}

	ServiceReference<T> getServiceReference() {
		return this.reference;
	}
}
