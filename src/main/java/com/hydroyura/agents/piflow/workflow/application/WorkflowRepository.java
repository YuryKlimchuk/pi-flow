package com.hydroyura.agents.piflow.workflow.application;

import java.util.Optional;

import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowId;

/**
 * Порт хранения Workflow.
 */
public interface WorkflowRepository {

	void save(Workflow workflow);

	Optional<Workflow> findById(WorkflowId id);

	java.util.List<Workflow> findAll();
}
