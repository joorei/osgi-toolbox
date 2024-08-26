package org.codeturnery.osgi.fixtures.bundles.a;

import org.codeturnery.osgi.fixtures.bundles.contract.Book;

public class BookImpl implements Book {

	private String authorName;
	private String title;
	
	public BookImpl(String authorName, String title) {
		this.authorName = authorName;
		this.title = title;
	}
	
	@Override
	public String getAuthorName() {
		return this.authorName;
	}

	@Override
	public String getTitle() {
		return this.title;
	}
}
