package com.hydroyura.agents.piflow.web;

import com.hydroyura.agents.piflow.github.api.IssueState;
import com.hydroyura.agents.piflow.github.api.IssueSummary;

/**
 * View-модель Issue для Thymeleaf-шаблонов.
 */
public record IssueView(long number, String title, String stateLabel, boolean open, String url) {

	public static IssueView from(IssueSummary summary) {
		boolean open = summary.state() == IssueState.OPEN;
		return new IssueView(summary.number(), summary.title(), open ? "Открыт" : "Закрыт", open, summary.url());
	}
}
