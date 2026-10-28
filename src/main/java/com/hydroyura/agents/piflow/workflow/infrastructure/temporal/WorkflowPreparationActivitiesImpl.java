package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import java.util.Map;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;

/**
 * Реализация activity подготовки workspace через публичный контракт модуля
 * {@code workspace}.
 *
 * <p>Activity не содержит JGit-кода и не вычисляет пути клонирования — все
 * локальные Git-операции делегируются {@link WorkspaceOperations#prepare}.
 * Эксклюзивная блокировка репозитория приобретается через
 * {@link WorkspaceLockOperations} и возвращается в результате для последующего
 * идемпотентного освобождения.</p>
 */
public class WorkflowPreparationActivitiesImpl implements WorkflowPreparationActivities {

	private final WorkspaceOperations workspaceOperations;
	private final WorkspaceLockOperations lockOperations;

	public WorkflowPreparationActivitiesImpl(WorkspaceOperations workspaceOperations) {
		this(workspaceOperations, null);
	}

	public WorkflowPreparationActivitiesImpl(WorkspaceOperations workspaceOperations, WorkspaceLockOperations lockOperations) {
		this.workspaceOperations = workspaceOperations;
		this.lockOperations = lockOperations;
	}

	@Override
	public WorkflowPreparationResult prepare(WorkflowInput input) {
		String branch = "pi-flow/" + input.workflowId();
		String baseBranch = workspaceOperations.resolveBaseBranch(input.repositoryUrl(), "master");
		WorkspacePreparationRequest request = new WorkspacePreparationRequest(
				input.workflowId(),
				input.repositoryUrl(),
				baseBranch,
				Map.of());
		WorkspaceResult result = workspaceOperations.prepare(request);

		String lockKey = null;
		String lockToken = null;
		if (lockOperations != null) {
			WorkspaceLock lock = lockOperations.acquire(input.repositoryUrl(), input.workflowId());
			lockKey = lock.repositoryKey();
			lockToken = lock.token();
		}
		return new WorkflowPreparationResult(
				result.path() != null ? result.path().toString() : null,
				result.branch() != null ? result.branch() : branch,
				lockKey,
				lockToken,
				baseBranch);
	}
}
