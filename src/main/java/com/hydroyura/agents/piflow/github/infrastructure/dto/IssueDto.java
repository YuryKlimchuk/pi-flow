package com.hydroyura.agents.piflow.github.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO задачи (Issue) GitHub. Поле {@code pullRequest} присутствует, когда элемент
 * на самом деле является pull request.
 */
public record IssueDto(
		long number,
		String title,
		String state,
		String body,
		AccountDto user,
		@JsonProperty("html_url") String htmlUrl,
		@JsonProperty("pull_request") Object pullRequest) {
}
