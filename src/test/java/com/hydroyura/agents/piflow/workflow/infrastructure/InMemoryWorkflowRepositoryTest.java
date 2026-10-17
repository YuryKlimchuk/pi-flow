package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import org.junit.jupiter.api.Test;

class InMemoryWorkflowRepositoryTest {

    @Test
    void save_findById_findAll() {
        InMemoryWorkflowRepository repository = new InMemoryWorkflowRepository();
        Workflow workflow = new Workflow(WorkflowId.generate(), "task", "/repo");

        repository.save(workflow);

        assertThat(repository.findById(workflow.getId())).contains(workflow);
        assertThat(repository.findAll()).containsExactly(workflow);
        assertThat(repository.findById(WorkflowId.generate())).isEmpty();
    }
}
