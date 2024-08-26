package org.codeturnery.osgi.toolbox.manager;

import java.time.Instant;

import org.eclipse.jdt.annotation.Checks;

class Expiration {
	private final Stage previousStage;
	private final Stage newStage;
	private final Instant switchTime;
	
	Expiration(final Stage previousStage, final Stage newStage) {
		this.previousStage = previousStage;
		this.newStage = newStage;
		this.switchTime = Checks.requireNonNull(Instant.now());
	}

	Stage getPreviousStage() {
		return this.previousStage;
	}

	Stage getNewStage() {
		return this.newStage;
	}

	Instant getSwitchTime() {
		return this.switchTime;
	}
}
