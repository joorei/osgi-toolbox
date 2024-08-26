package org.codeturnery.osgi.toolbox.manager;

import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Checks;
import org.osgi.framework.ServiceReference;

import org.codeturnery.proxies.LockableProxyFactory;

/**
 * This wrapper instance hides the actual service from the calling application.
 * This is necessary because references to service objects must be handled
 * carefully, to allow the garbage collector to clean them up.
 * <p>
 * When invoking the {@link #callService(Consumer)} method in this instance you
 * will be passed the actual service into your function.
 * <p>
 * <strong>Do not keep the reference to the service by saving it into your own
 * objects.</strong> You will get exceptions when calling its methods after your
 * {@link Consumer} function returned.
 *
 * @param <T>
 */
public class OsgiServiceWrapper<T> implements ServiceWrapper<T> {
	private final ServiceReference<T> reference;
	private final OsgiBundleRegistry bundleRegistry;
	private final Class<T> clazz;

	OsgiServiceWrapper(final ServiceReference<T> reference, final Class<T> clazz,
			final OsgiBundleRegistry bundleRegistry) {
		this.reference = Checks.requireNonNull(reference);
		this.clazz = Checks.requireNonNull(clazz);
		this.bundleRegistry = Checks.requireNonNull(bundleRegistry);
	}

	@Override
	public void callService(final Consumer<T> consumer) throws LoadServiceException {
		final T targetService = getService();
		final LockableProxyFactory<T> proxyFactory = new LockableProxyFactory<>(targetService, this.clazz);
		try {
			consumer.accept(proxyFactory.createProxy());
		} finally {
			proxyFactory.lock();
			this.bundleRegistry.releaseService(this.reference);
		}
	}

	T getService() throws LoadServiceException {
		final Optional<T> optionalService = this.bundleRegistry.loadBundleService(this.reference);
		if (optionalService.isEmpty()) {
			throw new LoadServiceException(this.clazz);
		}
		return optionalService.get();
	}

	ServiceReference<T> getServiceReference() {
		return this.reference;
	}
}
