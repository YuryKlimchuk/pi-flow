package com.hydroyura.agents.piflow.web;

import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Контроллер запуска Workflow по выбранной Issue.
 *
 * <p>Тонкий адаптер: принимает owner/repository/issue, вызывает публичный
 * {@link WorkflowOperations} и отображает результат. Не обращается к
 * internal-пакетам workflow.</p>
 */
@Controller
@RequiredArgsConstructor
public class WorkflowController {

	private final WorkflowOperations workflowOperations;

	@PostMapping("/workflows/start")
	public String start(@RequestParam String owner,
			@RequestParam String repository,
			@RequestParam("issueNumber") long issueNumber,
			@RequestParam(required = false) String workflowId,
			Model model) {
		model.addAttribute("owner", owner);
		model.addAttribute("repository", repository);
		model.addAttribute("issueNumber", issueNumber);

		String effectiveWorkflowId = (workflowId == null || workflowId.isBlank())
				? owner + "/" + repository + "#" + issueNumber
				: workflowId;
		StartWorkflowCommand command = new StartWorkflowCommand(effectiveWorkflowId, owner, repository, issueNumber);
		try {
			WorkflowState state = workflowOperations.start(command);
			model.addAttribute("state", state);
			model.addAttribute("error", null);
		} catch (WorkflowException e) {
			model.addAttribute("state", null);
			model.addAttribute("error", message(e));
		}
		return "workflow-start";
	}

	private String message(WorkflowException e) {
		return switch (e.errorType()) {
			case VALIDATION -> "Некорректные данные: " + e.getMessage();
			case NOT_FOUND -> "Workflow не найден: " + e.getMessage();
			case EXECUTION -> "Не удалось запустить исполнение: " + e.getMessage();
			case ILLEGAL_TRANSITION, ORCHESTRATION -> "Ошибка запуска: " + e.getMessage();
		};
	}
}
