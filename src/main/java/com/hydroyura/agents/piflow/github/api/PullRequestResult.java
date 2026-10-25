package com.hydroyura.agents.piflow.github.api;

/**
 * Результат создания или нахождения pull request.
 *
 * @param number номер pull request
 * @param url абсолютный URL pull request
 */
public record PullRequestResult(long number, String url) {
}
