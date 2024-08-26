package org.codeturnery.osgi.fixtures.bundles.a.importer;

import java.util.Arrays;
import java.util.Collection;

import org.codeturnery.osgi.fixtures.bundles.a.BookImpl;
import org.codeturnery.osgi.fixtures.bundles.contract.Book;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;

public class BookImporterImpl implements BookImporter {
	@Override
	public Collection<Book> getBooks() {
		return Arrays.asList(
				new BookImpl("Steven King", "Bag of Bones"),
				new BookImpl("Agatha Christie", "The Mysterious Affair at Styles"));
	}

	@Override
	public int getBookCount() {
		return getBooks().size();
	}
}
