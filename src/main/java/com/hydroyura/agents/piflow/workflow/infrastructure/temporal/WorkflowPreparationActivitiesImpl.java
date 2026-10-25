package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import java.util.Map;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;

/**
 * Реализация activity подготовки workspace через публичный контракт модуля
 * {@code workspace}.
 *
 * <p>Activity не содержит JGit-кода и не вычисляет пути клонирования — все
 * локальные Git-операции делегируются {@link WorkspaceOperations#prepare}.
 * Ветка формируется по идентификатору workflow как {@code pi-flow/<workflowId>}.</p>
 */
public class WorkflowPreparationActivitiesImpl implements WorkflowPreparationActivities {

	private final WorkspaceOperations workspaceOperations;

	public WorkflowPreparationActivitiesImpl(WorkspaceOperations workspaceOperations) {
		this.workspaceOperations = workspaceOperations;
	}

	@Override
	public WorkflowPreparationResult prepare(WorkflowInput input) {
		String branch = "pi-flow/" + input.workflowId();
		WorkspacePreparationRequest request = new WorkspacePreparationRequest(
				input.workflowId(),
				input.repositoryUrl(),
				"master",
				Map.of());
		WorkspaceResult result = workspaceOperations.prepare(request);
		return new WorkflowPreparationResult(
				result.path() != null ? result.path().toString() : null,
				result.branch() != null ? result.branch() : branch);
	}
}
