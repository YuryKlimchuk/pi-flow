package com.hydroyura.agents.piflow.workflow.domain;

import java.util.Objects;

/**
 * Значение идентификатора Workflow.
 *
 * <p>Не зависит от фреймворков и инфраструктурных деталей.</p>
 */
public final class WorkflowId {

	private final String value;

	public WorkflowId(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("workflow id must not be blank");
		}
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WorkflowId other)) {
			return false;
		}
		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return value;
	}
}
