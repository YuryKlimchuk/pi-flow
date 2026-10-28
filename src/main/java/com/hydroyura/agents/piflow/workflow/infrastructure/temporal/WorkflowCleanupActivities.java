package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Контракт activity освобождения ресурсов (workspace lock) внутри Temporal execution.
 */
@ActivityInterface
public interface WorkflowCleanupActivities {

	/**
	 * Идемпотентно освобождает блокировку репозитория по ключу и токену.
	 *
	 * @param lockKey стабильный ключ репозитория
	 * @param lockToken токен владельца блокировки
	 */
	@ActivityMethod
	void releaseLock(String lockKey, String lockToken);
}
