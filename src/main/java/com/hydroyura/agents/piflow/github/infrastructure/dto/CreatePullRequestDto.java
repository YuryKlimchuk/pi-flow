package com.hydroyura.agents.piflow.github.infrastructure.dto;

/**
 * Тело запроса создания pull request.
 */
public record CreatePullRequestDto(String title, String body, String head, String base) {
}
