package org.codeturnery.osgi.toolbox.manager;

import org.codeturnery.plugin.CallServiceException;
import org.codeturnery.plugin.Registry;
import org.osgi.framework.ServiceReference;

public class ServiceReferenceException extends CallServiceException {

	private final ServiceReference<?> reference;
	
	ServiceReferenceException(final ServiceReference<?> reference, final Class<?> clazz, final Registry registry) {
		super("No service found for given reference in this registry.", clazz, registry);
		this.reference = reference;
	}

	public ServiceReference<?> getReference() {
		return this.reference;
	}
}
