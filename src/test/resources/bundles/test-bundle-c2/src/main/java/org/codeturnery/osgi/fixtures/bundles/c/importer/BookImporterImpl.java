package org.codeturnery.osgi.fixtures.bundles.c.importer;

import java.util.Arrays;
import java.util.Collection;

import org.codeturnery.osgi.fixtures.bundles.c.BookImpl;
import org.codeturnery.osgi.fixtures.bundles.contract.Book;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;

public class BookImporterImpl implements BookImporter {
	private final boolean lastNameOnly;

	public BookImporterImpl(boolean lastNameOnly) {
		this.lastNameOnly = lastNameOnly;
	}

	@Override
	public Collection<Book> getBooks() {
		return Arrays.asList(
				new BookImpl((this.lastNameOnly ? "" : "Steven") + "King", "The Dark Tower"),
				new BookImpl((this.lastNameOnly ? "" : "J. R. R.") +" Tolkien", "The Hobbit"));
	}

	@Override
	public int getBookCount() {
		return this.getBooks().size();
	}
}
