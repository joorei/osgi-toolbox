package org.codeturnery.osgi.toolbox.manager;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Builds all test JARs relevant for test execution (overwriting existing ones).
 *
 * Each JAR is only build once during a single JUnit run, even if this extension
 * is used multiple times throughout the unit tests.
 */
public class JarBuilderExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

	private static boolean started = false;

	@Override
	public void beforeAll(@Nullable ExtensionContext context) throws MavenInvocationException, CommandLineException {
		if (!started) {
			started = true;

			buildJar(BundleTest.BUNDLES_TEST_DIR.resolve("test-bundle-contract1"), "test-bundle-contract-1.0.0.jar",
					"install");
			buildJar(BundleTest.BUNDLES_TEST_DIR.resolve("test-bundle-contract2"), "test-bundle-contract-2.0.0.jar",
					"install");

			buildJar(BundleTest.A);
			buildJar(BundleTest.B);
			buildJar(BundleTest.C1);
			buildJar(BundleTest.C2);

			// The following line registers a callback hook when the root test context is
			// shut down
			context.getRoot().getStore(GLOBAL).put("any unique name", this);
		}
	}

	@Override
	public void close() {
		// Your "after all tests" logic goes here
	}

	private static Path buildJar(final BundleProject project) throws MavenInvocationException, CommandLineException {
		return buildJar(project.getProjectPath(), project.getJarName(), "package");
	}

	private static Path buildJar(final Path projectPath, final String jarName, final String goal)
			throws MavenInvocationException, CommandLineException {
		final Path jarPath = projectPath.resolve("target").resolve(jarName);

		final Invoker invoker = new DefaultInvoker();
		final InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(projectPath.resolve("pom.xml").toFile());
		request.setGoals(Collections.singletonList(goal));
		invoker.setMavenHome(new File("/usr"));
		final InvocationResult result = invoker.execute(request);
		final CommandLineException exception = result.getExecutionException();
		if (exception != null) {
			throw exception;
		}

		return jarPath;
	}
}