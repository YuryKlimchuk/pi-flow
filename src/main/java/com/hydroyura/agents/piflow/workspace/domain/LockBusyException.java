package com.hydroyura.agents.piflow.workspace.domain;

import java.time.Instant;

/**
 * Ошибка занятого репозитория (внутренняя модель).
 */
public class LockBusyException extends RuntimeException {

	private final String owner;
	private final Instant expiresAt;

	public LockBusyException(String owner, Instant expiresAt) {
		super("repository is locked by '" + owner + "' until " + expiresAt);
		this.owner = owner;
		this.expiresAt = expiresAt;
	}

	public String owner() {
		return owner;
	}

	public Instant expiresAt() {
		return expiresAt;
	}
}
