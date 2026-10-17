package com.hydroyura.agents.piflow.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void create_shouldStartAtTaskSetupAndInProgress() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        assertThat(workflow.getCurrentStage()).isEqualTo(WorkflowStage.TASK_SETUP);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        assertThat(workflow.outcomes()).isEmpty();
    }

    @Test
    void recordOutcome_moveTo_markAwaitingUser_shouldUpdateState() {
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");
        workflow.recordOutcome(StageOutcome.success(WorkflowStage.TASK_SETUP, ""));
        workflow.moveTo(WorkflowStage.PROJECT_ANALYSIS);
        workflow.markAwaitingUser();

        assertThat(workflow.outcomes()).hasSize(1);
        assertThat(workflow.getCurrentStage()).isEqualTo(WorkflowStage.PROJECT_ANALYSIS);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.AWAITING_USER);
        assertThat(workflow.isTerminal()).isFalse();
    }
}
