package com.hydroyura.agents.piflow.workflow.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.hydroyura.agents.piflow.workflow.application.WorkflowRepository;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowId;
import org.springframework.stereotype.Component;

/**
 * In-memory хранилище Workflow для стадии минимального запуска.
 *
 * <p>Не предназначено для production: состояние не переживает перезапуск
 * приложения. Вводится как явное ограничение текущего изменения.</p>
 */
@Component
public class InMemoryWorkflowRepository implements WorkflowRepository {

	private final Map<WorkflowId, Workflow> store = new ConcurrentHashMap<>();

	@Override
	public void save(Workflow workflow) {
		store.put(workflow.id(), workflow);
	}

	@Override
	public Optional<Workflow> findById(WorkflowId id) {
		return Optional.ofNullable(store.get(id));
	}
}
