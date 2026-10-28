package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-тест activity освобождения lock.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowCleanupActivitiesImplTest {

	@Mock
	private WorkspaceLockOperations lockOperations;

	@Test
	void releaseLock_shouldDelegate() {
		WorkflowCleanupActivitiesImpl activity = new WorkflowCleanupActivitiesImpl(lockOperations);

		activity.releaseLock("repo-key", "token-1");

		verify(lockOperations).release(any(WorkspaceLock.class));
	}

	@Test
	void releaseLock_withMissingToken_shouldSkip() {
		WorkflowCleanupActivitiesImpl activity = new WorkflowCleanupActivitiesImpl(lockOperations);

		activity.releaseLock("repo-key", null);

		verify(lockOperations, never()).release(any());
	}
}
