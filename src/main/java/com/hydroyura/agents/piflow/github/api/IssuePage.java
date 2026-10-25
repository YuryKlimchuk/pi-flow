package com.hydroyura.agents.piflow.github.api;

import java.util.List;

/**
 * Страница задач (Issues) с метаданными пагинации.
 */
public record IssuePage(List<IssueSummary> items, int page, int perPage, boolean hasNext) {
}
