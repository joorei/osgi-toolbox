package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.nio.file.Path;

public class BundleProject {

	private final String jarName;
	private final String projectDirectoryName;
	private final String identifier;

	public BundleProject(final String jarName, final String projectDirectoryName, final String identifier) {
		this.jarName = jarName;
		this.projectDirectoryName = projectDirectoryName;
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public String getJarName() {
		return this.jarName;
	}
	
	public File getJarFile() {
		return getProjectPath().resolve("target").resolve(this.jarName).toFile();
	}

	public Path getProjectPath() {
		return BundleTest.BUNDLES_TEST_DIR.resolve(this.projectDirectoryName);
	}
}
