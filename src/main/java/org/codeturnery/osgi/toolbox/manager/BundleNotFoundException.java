package org.codeturnery.osgi.toolbox.manager;

public class BundleNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	BundleNotFoundException() {
		super();
	}

	BundleNotFoundException(final String message, final Throwable cause) {
		super(message, cause);
	}

	BundleNotFoundException(final String message) {
		super(message);
	}

	BundleNotFoundException(final Throwable cause) {
		super(cause);
	}
	
}
