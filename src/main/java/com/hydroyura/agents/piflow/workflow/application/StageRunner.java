package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;

/** Port for executing a single workflow stage. */
public interface StageRunner {

    StageOutcome run(WorkflowStage stage, Workflow workflow);
}
