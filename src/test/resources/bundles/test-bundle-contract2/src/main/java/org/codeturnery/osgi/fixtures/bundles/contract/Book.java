package org.codeturnery.osgi.fixtures.bundles.contract;

/**
 * @since 1.0.0
 */
public interface Book {
	/**
	 * @since 1.0.0
	 *
	 * @return The title of this book instance.
	 */
	public String getTitle();

	/**
	 * @since 1.1.0
	 *
	 * @return The name of the author of this book instance.
	 */
	public String getAuthorName();
}
