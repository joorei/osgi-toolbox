package org.codeturnery.osgi.toolbox.manager;

public class RegistrationException extends StageChangeException {

	private static final long serialVersionUID = -5310702196640516548L;

	RegistrationException(final String message) {
		super(message);
	}

	RegistrationException(final Throwable cause) {
		super(cause);
	}

}
