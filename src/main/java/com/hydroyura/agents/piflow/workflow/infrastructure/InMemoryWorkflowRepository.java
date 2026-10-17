package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.application.WorkflowRepository;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory Map-backed workflow repository. */
@Component
public class InMemoryWorkflowRepository implements WorkflowRepository {

    private final Map<WorkflowId, Workflow> store = new ConcurrentHashMap<>();

    @Override
    public void save(Workflow workflow) {
        store.put(workflow.getId(), workflow);
    }

    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Workflow> findAll() {
        return List.copyOf(store.values());
    }
}
