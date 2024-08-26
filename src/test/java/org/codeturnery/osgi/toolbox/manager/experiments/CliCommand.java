package org.codeturnery.osgi.toolbox.manager.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codeturnery.osgi.toolbox.manager.InstallationException;
import org.codeturnery.osgi.toolbox.manager.OsgiBundleRegistry;
import org.codeturnery.osgi.toolbox.manager.BundleConflict;
import org.codeturnery.osgi.toolbox.manager.RegisteredBundle;
import org.codeturnery.osgi.toolbox.manager.RegistrationException;
import org.osgi.framework.BundleException;

@SuppressWarnings({"null", "nls", "unused"})
public class CliCommand {
	public static void main(String[] args) throws IOException, BundleException, RegistrationException, InstallationException {
		final var options = new Options();

		final var filesOption = new Option("f", "files", true, "path to bundle file");
		// not implemented in version apache commons cli 1.5.0
		//final var filesType = PatternOptionBuilder.FILES_VALUE;
		//filesOption.setType(filesType);
		filesOption.setRequired(true);
		filesOption.setOptionalArg(false);
		filesOption.setArgs(Integer.MAX_VALUE);
		options.addOption(filesOption);

		final var dependenciesOption = new Option("d", "dependencies", true, "dependencies needed by the bundles");
		dependenciesOption.setRequired(false);
		dependenciesOption.setOptionalArg(false);
		dependenciesOption.setType(String.class);
		dependenciesOption.setArgs(Integer.MAX_VALUE);
		options.addOption(dependenciesOption);

		final CommandLineParser parser = new DefaultParser();
		final var formatter = new HelpFormatter();

		try {
			final CommandLine cmd = parser.parse(options, args);
			final File[] files = getFiles(cmd, filesOption);
			final Set<String> dependencies = getDependencies(cmd, dependenciesOption);
			try (final var bundleRegistry = new OsgiBundleRegistry(dependencies);) {
				for (final File file : files) {
					bundleRegistry.registerBundle(file);
				}
				final List<RegisteredBundle> bundles = bundleRegistry.getBundles();
				printConflicts(bundles);
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);

			System.exit(1);
		}
	}

	private static void printConflicts(final List<RegisteredBundle> bundles) {
		System.out.println("==== CONFLICTS ====");
		for (final RegisteredBundle currentBundle : bundles) {
			System.out.println("Bundle: " + currentBundle.getSymbolicNameWithVersion());
			int i = 1;
			for (final BundleConflict conflict : currentBundle.getConflicts()) {
				final Set<String> classes = conflict.getConflictingClasses();
				final String nameConflictString = conflict.isConflictingSymbolicName()
						? " in name and"
						: "";
				System.out.println("#" + (i++) + " " + conflict.getConflictingBundle().getSymbolicNameWithVersion() + ": " + nameConflictString + " in " + classes.size() + " class(es):");
				for (final String conflictingClass : classes) {
					System.out.println(" • " + conflictingClass);
				}
			}
		}
	}

	private static File[] getFiles(final CommandLine cmd, final Option filesOption) {
		// not implemented in version apache commons cli 1.5.0
		//return filesType.cast(cmd.getParsedOptionValue(filesOption));
		final String[] fileStrings = cmd.getOptionValues(filesOption);
		final File[] files = new File[fileStrings.length];
		for (int i = 0; i < fileStrings.length; i++) {
			files[i] = new File(fileStrings[i]);
		}
		return files;
	}
	
	private static Set<String> getDependencies(final CommandLine cmd, final Option dependenciesOption) {
		final String[] dependencyStrings = cmd.hasOption(dependenciesOption)
				? cmd.getOptionValues(dependenciesOption)
				: new String[] {};
		final List<String> dependencyList = dependencyStrings.length == 0
				? Collections.emptyList()
				: Arrays.asList(dependencyStrings);
		return new HashSet<>(dependencyList);
	}
}
