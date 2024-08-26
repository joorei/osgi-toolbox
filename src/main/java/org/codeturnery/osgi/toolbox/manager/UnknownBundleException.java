package org.codeturnery.osgi.toolbox.manager;

public class UnknownBundleException extends RuntimeException {

	private static final long serialVersionUID = 3322120037050691352L;

	UnknownBundleException(final String message) {
		super(message);
	}
}
