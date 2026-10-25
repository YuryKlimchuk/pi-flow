package com.hydroyura.agents.piflow.github.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO репозитория GitHub.
 */
public record RepoDto(
		long id,
		AccountDto owner,
		String name,
		@JsonProperty("html_url") String htmlUrl) {
}
