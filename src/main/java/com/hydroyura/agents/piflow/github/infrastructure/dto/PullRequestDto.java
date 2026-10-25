package com.hydroyura.agents.piflow.github.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO pull request GitHub.
 */
public record PullRequestDto(
		long number,
		String state,
		@JsonProperty("html_url") String htmlUrl,
		RefDto head,
		RefDto base) {
}
