package org.codeturnery.osgi.fixtures.bundles.b.importer;

import java.util.Arrays;
import java.util.Collection;

import org.codeturnery.osgi.fixtures.bundles.b.BookImpl;
import org.codeturnery.osgi.fixtures.bundles.contract.Book;
import org.codeturnery.osgi.fixtures.bundles.contract.BookImporter;

public class BookImporterImpl implements BookImporter {
	@Override
	public Collection<Book> getBooks() {
		return Arrays.asList(
				new BookImpl("It"),
				new BookImpl("The Fellowship of the Ring"));
	}
	
	
}
