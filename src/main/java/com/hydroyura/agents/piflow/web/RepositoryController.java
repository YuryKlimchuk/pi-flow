package com.hydroyura.agents.piflow.web;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.api.RepositoryOperations;
import com.hydroyura.agents.piflow.github.api.RepositoryPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Контроллер страницы доступных репозиториев GitHub.
 */
@Controller
@RequiredArgsConstructor
public class RepositoryController {

	private final RepositoryOperations repositoryOperations;

	@GetMapping("/repositories")
	public String repositories(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "30") int perPage,
			Model model) {
		try {
			RepositoryPage result = repositoryOperations.listRepositories(PageRequest.of(page, perPage));
			List<RepositoryView> items = result.items().stream().map(RepositoryView::from).toList();
			model.addAttribute("repositories", items);
			model.addAttribute("page", result.page());
			model.addAttribute("perPage", result.perPage());
			model.addAttribute("hasNext", result.hasNext());
			model.addAttribute("hasPrevious", result.page() > 1);
			model.addAttribute("error", null);
		} catch (GithubException e) {
			model.addAttribute("repositories", List.of());
			model.addAttribute("page", page);
			model.addAttribute("perPage", perPage);
			model.addAttribute("hasNext", false);
			model.addAttribute("hasPrevious", page > 1);
			model.addAttribute("error", GithubUiMessages.message(e));
			model.addAttribute("retryable", e.retryable());
		}
		return "repositories";
	}
}
