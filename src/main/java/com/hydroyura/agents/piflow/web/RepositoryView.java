package com.hydroyura.agents.piflow.web;

import com.hydroyura.agents.piflow.github.api.RepositorySummary;

/**
 * View-модель репозитория для Thymeleaf-шаблонов.
 */
public record RepositoryView(long id, String owner, String name, String url) {

	public static RepositoryView from(RepositorySummary summary) {
		return new RepositoryView(summary.id(), summary.owner(), summary.name(), summary.url());
	}
}
