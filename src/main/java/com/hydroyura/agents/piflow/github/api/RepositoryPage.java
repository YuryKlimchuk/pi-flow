package com.hydroyura.agents.piflow.github.api;

import java.util.List;

/**
 * Страница репозиториев с метаданными пагинации.
 */
public record RepositoryPage(List<RepositorySummary> items, int page, int perPage, boolean hasNext) {
}
