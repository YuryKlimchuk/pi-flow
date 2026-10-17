package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.List;
import java.util.Optional;

/** Persistence port for workflow runs. */
public interface WorkflowRepository {

    void save(Workflow workflow);

    Optional<Workflow> findById(WorkflowId id);

    List<Workflow> findAll();
}
