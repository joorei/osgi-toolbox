package org.codeturnery.osgi.toolbox.manager;

public class LoadServiceException extends RuntimeException {

	private static final long serialVersionUID = 4886623970702082407L;
	private final Class<?> clazz;

	LoadServiceException(final Class<?> clazz, final Throwable cause) {
		super(cause);
		this.clazz = clazz;
	}
	
	LoadServiceException(final Class<?> clazz) {
		super();
		this.clazz = clazz;
	}

	public Class<?> getClazz() {
		return this.clazz;
	}
}
