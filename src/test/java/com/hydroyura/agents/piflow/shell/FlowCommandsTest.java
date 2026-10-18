package com.hydroyura.agents.piflow.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hydroyura.agents.piflow.workflow.api.WorkflowEngine;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowView;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlowCommandsTest {

    @Test
    void retry_withoutAcknowledgementFlag_shouldPassFalseToEngine() {
        WorkflowEngine engine = mock(WorkflowEngine.class);
        WorkflowId id = WorkflowId.generate();
        WorkflowView view = new WorkflowView(id, "task", "/repo", WorkflowStage.REVIEW, WorkflowStatus.PAUSED, List.of());
        when(engine.retryStage(id, false)).thenReturn(view);

        FlowCommands commands = new FlowCommands(engine);
        String output = commands.retry(id.value().toString(), false);

        verify(engine).retryStage(id, false);
        assertThat(output).contains("status=" + WorkflowStatus.PAUSED);
    }

    @Test
    void retry_withAcknowledgementFlag_shouldPassTrueToEngine() {
        WorkflowEngine engine = mock(WorkflowEngine.class);
        WorkflowId id = WorkflowId.generate();
        WorkflowView view = new WorkflowView(id, "task", "/repo", WorkflowStage.IMPLEMENTATION, WorkflowStatus.PAUSED, List.of());
        when(engine.retryStage(id, true)).thenReturn(view);

        FlowCommands commands = new FlowCommands(engine);
        commands.retry(id.value().toString(), true);

        verify(engine).retryStage(id, true);
    }
}
