package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Реализация activity подготовки Workspace для minimal-сценария.
 *
 * <p>Возвращает маркер результата выполнения activity. Реальная координация
 * workspace/agent задаётся через application-слой, а не внутри activity.</p>
 */
public class WorkflowPreparationActivitiesImpl implements WorkflowPreparationActivities {

	@Override
	public String prepare(String workflowId, String repositoryUrl, long issueNumber) {
		return "prepared:" + workflowId;
	}
}
