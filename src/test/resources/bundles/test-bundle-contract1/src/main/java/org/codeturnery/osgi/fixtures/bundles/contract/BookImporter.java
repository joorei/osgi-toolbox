package org.codeturnery.osgi.fixtures.bundles.contract;

import java.util.Collection;

/**
 * @since 1.0.0
 */
public interface BookImporter {
	/**
	 * @since 1.0.0
	 *
	 * @return A collection of all books that can be provided by this importer instance.
	 */
	public Collection<Book> getBooks();
}
