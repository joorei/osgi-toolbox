package org.codeturnery.osgi.toolbox.manager;

abstract class AbstractBundleRegistry implements BundleRegistry {
	@Override
	public boolean isNotInstalled(final RegisteredBundle bundle) {
		return !(bundle instanceof InstalledBundle);
	}

	@Override
	public boolean isInstalledButNotStarted(final RegisteredBundle bundle) {
		return bundle instanceof InstalledBundle && !isStarted(bundle);
	}

	@Override
	public boolean isStarted(final RegisteredBundle bundle) {
		return bundle instanceof StartedBundle;
	}
}
