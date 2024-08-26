package org.codeturnery.osgi.toolbox.manager;

import java.util.function.Consumer;

public interface ServiceWrapper<T> {
	public void callService(final Consumer<T> serviceConsumer) throws LoadServiceException;
}
