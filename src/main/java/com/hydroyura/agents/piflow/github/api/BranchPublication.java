package com.hydroyura.agents.piflow.github.api;

/**
 * Результат публикации подготовленной ветки в GitHub.
 *
 * @param ref полное имя ref удалённой ветки
 * @param url URL удалённой ветки
 */
public record BranchPublication(String ref, String url) {
}
