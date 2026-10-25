package com.hydroyura.agents.piflow.github.api;

/**
 * Параметры страницы для пагинированных операций GitHub.
 *
 * @param page номер страницы, начиная с 1
 * @param perPage количество элементов на странице
 */
public record PageRequest(int page, int perPage) {

	public static PageRequest of(int page, int perPage) {
		return new PageRequest(page, perPage);
	}
}
