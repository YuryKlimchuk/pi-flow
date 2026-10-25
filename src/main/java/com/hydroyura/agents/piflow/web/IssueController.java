package com.hydroyura.agents.piflow.web;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.IssueFilter;
import com.hydroyura.agents.piflow.github.api.IssueOperations;
import com.hydroyura.agents.piflow.github.api.IssuePage;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Контроллер страницы Issues выбранного репозитория GitHub.
 */
@Controller
@RequiredArgsConstructor
public class IssueController {

	private final IssueOperations issueOperations;

	@GetMapping("/repositories/{owner}/{repository}/issues")
	public String issues(@PathVariable String owner,
			@PathVariable String repository,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "30") int perPage,
			Model model) {
		model.addAttribute("owner", owner);
		model.addAttribute("repository", repository);
		try {
			IssuePage result = issueOperations.listIssues(owner, repository, IssueFilter.ALL, PageRequest.of(page, perPage));
			List<IssueView> items = result.items().stream().map(IssueView::from).toList();
			model.addAttribute("issues", items);
			model.addAttribute("page", result.page());
			model.addAttribute("perPage", result.perPage());
			model.addAttribute("hasNext", result.hasNext());
			model.addAttribute("hasPrevious", result.page() > 1);
			model.addAttribute("error", null);
		} catch (GithubException e) {
			model.addAttribute("issues", List.of());
			model.addAttribute("page", page);
			model.addAttribute("perPage", perPage);
			model.addAttribute("hasNext", false);
			model.addAttribute("hasPrevious", page > 1);
			model.addAttribute("error", GithubUiMessages.message(e));
			model.addAttribute("retryable", e.retryable());
		}
		return "issues";
	}
}
