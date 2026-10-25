package com.hydroyura.agents.piflow.workspace.domain;

import java.util.Optional;

/**
 * Внутренний driven-порт хранилища блокировок.
 */
public interface LockStore {

	Lease acquire(String key, String owner);

	void release(String key, String token);

	Optional<Lease> peek(String key);
}
