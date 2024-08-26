package org.codeturnery.osgi.fixtures.bundles.b;

import org.codeturnery.osgi.fixtures.bundles.contract.Book;

public class BookImpl implements Book {

	private String title;
	
	public BookImpl(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return this.title;
	}
}
