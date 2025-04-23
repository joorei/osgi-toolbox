package org.codeturnery.osgi.toolbox.manager;

public class FrameworkStateException extends FrameworkException {

	private static final long serialVersionUID = 6395524098367378782L;

	FrameworkStateException(final int frameworkState) {
		super(frameworkState);
	}
	
}
