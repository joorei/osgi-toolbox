package org.codeturnery.osgi.toolbox.manager;

import org.osgi.framework.Bundle;

/**
 * For some reason, the framework can't be used as expected. Inheriting classes
 * may specify the problem.
 */
public class FrameworkException extends Exception {

	private static final long serialVersionUID = -7337742861456863054L;

	private final int frameworkState;

	FrameworkException(final int frameworkState) {
		this.frameworkState = frameworkState;
	}

	/**
	 * @return The state the framework was in when the problem occurred, as defined
	 *         in the {@link Bundle} constants.
	 */
	public int getFrameworkState() {
		return this.frameworkState;
	}
}
