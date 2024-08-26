package org.codeturnery.osgi.toolbox.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleException;

@SuppressWarnings("nls")
@ExtendWith({JarBuilderExtension.class})
abstract public class BundleTest {
	public static final Path BUNDLES_TEST_DIR = Paths.get("src", "test", "resources", "bundles");
	public final static BundleProject A = new BundleProject("test-bundle-a-1.0.0.jar", "test-bundle-a", "org.codeturnery.test-bundle-a:1.0.0");
	public final static BundleProject B = new BundleProject("test-bundle-b-1.0.0.jar", "test-bundle-b", "org.codeturnery.test-bundle-b:1.0.0");
	public final static BundleProject C1 = new BundleProject("test-bundle-c-1.0.0.jar", "test-bundle-c1", "org.codeturnery.test-bundle-c:1.0.0");
	public final static BundleProject C2 = new BundleProject("test-bundle-c-2.0.0.jar", "test-bundle-c2", "org.codeturnery.test-bundle-c:2.0.0");
	
	protected static final Set<String> C_CONFLICTS = Set.of("org/codeturnery/osgi/fixtures/bundles/c/BookImpl.class");
	
	@SuppressWarnings("null")
	protected OsgiBundleRegistry bundleRegistry = null;

	@BeforeEach
	void init() throws BundleException {
		final Set<String> extras = new HashSet<>(Arrays.asList("org.codeturnery.osgi.fixtures.bundles.contract; version=1.1.0, javassist, javassist.bytecode"));
		this.bundleRegistry = new OsgiBundleRegistry(extras);
	}

	@AfterEach
	void tearDown() throws IOException {
		this.bundleRegistry.close();
	}

	@SuppressWarnings("null")
	protected List<File> getBundleJarFiles() {
		return List.of(A, B, C1, C2).stream().map(BundleProject::getJarFile).collect(Collectors.toList());
	}
}
