package org.codeturnery.osgi.toolbox.manager.experiments;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.jdt.annotation.NonNull;

@SuppressWarnings({"null", "nls", "unused", "static-method"})
public class JarManifestPrinter {

	public void printJarInfos(final @NonNull File[] bundleFiles) throws IOException {
		for (int i = 0; i < bundleFiles.length; i++) {
			final File file = bundleFiles[i];
			try (final JarFile jar = new JarFile(file, true);) {
				final Manifest manifest = jar.getManifest();
				System.out.println();
				System.out.println();
				System.out.println("###" + jar.getName() + "###");
				System.out.println("Main Attributes:");
				final var mainAttributes = manifest.getMainAttributes();
				printAttributes(mainAttributes);
				
				System.out.println();
				System.out.println("Entries");
				final var entries = manifest.getEntries();
				for (final Entry<String, Attributes> entry : entries.entrySet()) {
					final Attributes value = entry.getValue();
					System.out.println(entry.getKey());
				}

				System.out.println();
				System.out.println("Export-Package:");
				final var exportPackageAttributes = manifest.getAttributes("Export-Package");
				if (exportPackageAttributes == null) {
					System.out.println("Export-Package is null");
					continue;
				}
				for (final Entry<Object, Object> e : exportPackageAttributes.entrySet()) {
					Object key = e.getKey();
					Object value = e.getValue();
					System.out.println(key + ": " + value);
				}
			}
		}
	}
	
	private void printAttributes(final Attributes attributes) {
		for (final Object k : attributes.keySet()) {
			if (k instanceof Attributes.Name) {
				final var nameKey = (Attributes.Name) k;
				final var attributeValue = attributes.getValue(nameKey);
				System.out.println(k + ": " + attributeValue);
			} else {
				throw new IllegalArgumentException("Unsupported key class: " + k.getClass());
			}
		}
	}

}
