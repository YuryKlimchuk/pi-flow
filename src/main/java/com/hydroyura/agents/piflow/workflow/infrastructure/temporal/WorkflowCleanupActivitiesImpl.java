package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;

/**
 * Реализация activity освобождения workspace lock через публичный контракт
 * модуля {@code workspace}. Освобождение идемпотентно: повторный вызов с тем же
 * токеном не затрагивает чужую блокировку.
 */
public class WorkflowCleanupActivitiesImpl implements WorkflowCleanupActivities {

	private final WorkspaceLockOperations lockOperations;

	public WorkflowCleanupActivitiesImpl(WorkspaceLockOperations lockOperations) {
		this.lockOperations = lockOperations;
	}

	@Override
	public void releaseLock(String lockKey, String lockToken) {
		if (lockKey == null || lockKey.isBlank() || lockToken == null || lockToken.isBlank()) {
			return;
		}
		lockOperations.release(new WorkspaceLock(lockKey, null, lockToken, null));
	}
}
