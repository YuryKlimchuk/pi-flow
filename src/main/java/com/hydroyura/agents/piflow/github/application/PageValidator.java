package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.PageRequest;

/**
 * Проверяет параметры страницы до HTTP-вызова.
 */
public final class PageValidator {

	public static final int MAX_PER_PAGE = 100;

	private PageValidator() {
	}

	public static void validate(PageRequest page) {
		if (page == null) {
			throw GithubException.validation("pagination", 0, "page must not be null");
		}
		if (page.page() < 1) {
			throw GithubException.validation("pagination", 0, "page must be greater than or equal to 1");
		}
		if (page.perPage() < 1 || page.perPage() > MAX_PER_PAGE) {
			throw GithubException.validation("pagination", 0,
					"perPage must be between 1 and " + MAX_PER_PAGE);
		}
	}
}
