package org.codeturnery.osgi.toolbox.manager;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

public class BundleConflict {
	private final RegisteredBundle conflictingBundle;
	private final Set<String> conflictingClasses;
	private final boolean conflictingSymbolicName;

	BundleConflict(final RegisteredBundle conflictingBundle, final Set<String> conflictingClasses,
			final boolean symbolicNameConflict) {
		this.conflictingBundle = conflictingBundle;
		this.conflictingSymbolicName = symbolicNameConflict;
		this.conflictingClasses = conflictingClasses;
	}

	public Set<String> getConflictingClasses() {
		return this.conflictingClasses;
	}

	public RegisteredBundle getConflictingBundle() {
		return this.conflictingBundle;
	}

	public boolean isConflictingSymbolicName() {
		return this.conflictingSymbolicName;
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == null || !(obj instanceof BundleConflict)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		final BundleConflict other = (BundleConflict) obj;
		
		return this.conflictingSymbolicName == other.conflictingSymbolicName
				&& this.conflictingClasses.equals(other.conflictingClasses)
				&& this.conflictingBundle.equals(other.conflictingBundle);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.conflictingClasses, this.conflictingBundle) + (this.conflictingSymbolicName ? 1 : 0);
	}
}
