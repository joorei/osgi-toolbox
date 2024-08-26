package org.codeturnery.osgi.toolbox.manager;

import java.io.File;

public class BundleAlreadyRegisteredException extends Exception {

	private static final long serialVersionUID = -6165645102040527991L;
	private final File unregisteredBundle;
	private final RegisteredBundle registeredBundle;

	BundleAlreadyRegisteredException(final File jarFile, final RegisteredBundle registeredBundle) {
		super("A bundle was already registered using the given path.");
		this.unregisteredBundle = jarFile;
		this.registeredBundle = registeredBundle;
	}

	public File getUnregisteredBundle() {
		return this.unregisteredBundle;
	}

	public RegisteredBundle getRegisteredBundle() {
		return this.registeredBundle;
	}
}
