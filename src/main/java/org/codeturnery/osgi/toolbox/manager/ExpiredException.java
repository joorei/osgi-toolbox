package org.codeturnery.osgi.toolbox.manager;

public class ExpiredException extends RuntimeException {

	private static final long serialVersionUID = -4561578981151228764L;
	private final Expiration expiration;

	ExpiredException(final Expiration expiration) {
		this.expiration = expiration;
	}

	public Expiration getExpiration() {
		return this.expiration;
	}
}
