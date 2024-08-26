package org.codeturnery.osgi.toolbox.manager;

public class BundleViolationException extends RegistrationException {

	private static final long serialVersionUID = -8639992765007187491L;

	BundleViolationException(final String message) {
		super(message);
	}
}
