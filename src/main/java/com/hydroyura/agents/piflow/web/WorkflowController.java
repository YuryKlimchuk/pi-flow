package com.hydroyura.agents.piflow.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.hydroyura.agents.piflow.workflow.api.ContinueWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.ReworkWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Контроллер запуска Workflow по выбранной Issue и отслеживания результата.
 *
 * <p>Тонкий адаптер: принимает owner/repository/issue, вызывает публичный
 * {@link WorkflowOperations} и отображает результат. Не обращается к
 * internal-пакетам workflow и не знает о Temporal.</p>
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
		String effectiveWorkflowId = (workflowId == null || workflowId.isBlank())
				? owner + "/" + repository + "#" + issueNumber
				: workflowId;
		StartWorkflowCommand command = new StartWorkflowCommand(effectiveWorkflowId, owner, repository, issueNumber);
		try {
			workflowOperations.start(command);
			return "redirect:/workflows/track?workflowId=" + encode(effectiveWorkflowId);
		} catch (WorkflowException e) {
			model.addAttribute("owner", owner);
			model.addAttribute("repository", repository);
			model.addAttribute("issueNumber", issueNumber);
			model.addAttribute("error", message(e));
			return "workflow-start";
		}
	}

	@GetMapping("/workflows")
	public String list(Model model) {
		model.addAttribute("workflows", workflowOperations.listWorkflows());
		return "workflow-list";
	}

	@GetMapping("/workflows/track")
	public String track(@RequestParam("workflowId") String workflowId, Model model) {
		model.addAttribute("workflowId", workflowId);
		try {
			WorkflowState state = workflowOperations.getState(workflowId);
			model.addAttribute("state", state);
			model.addAttribute("error", null);
		} catch (WorkflowException e) {
			model.addAttribute("state", null);
			model.addAttribute("error", message(e));
		}
		return "workflow-status";
	}

	@PostMapping("/workflows/continue")
	public String continueWorkflow(@RequestParam("workflowId") String workflowId,
			@RequestParam("branchName") String branchName) {
		workflowOperations.continueWorkflow(new ContinueWorkflowCommand(workflowId, branchName));
		return "redirect:/workflows/track?workflowId=" + encode(workflowId);
	}

	@PostMapping("/workflows/rework")
	public String rework(@RequestParam("workflowId") String workflowId,
			@RequestParam("feedback") String feedback, Model model) {
		try {
			workflowOperations.rework(new ReworkWorkflowCommand(workflowId, feedback));
			return "redirect:/workflows/track?workflowId=" + encode(workflowId);
		} catch (WorkflowException e) {
			model.addAttribute("workflowId", workflowId);
			model.addAttribute("state", currentState(workflowId));
			model.addAttribute("error", message(e));
			return "workflow-status";
		}
	}

	private WorkflowState currentState(String workflowId) {
		try {
			return workflowOperations.getState(workflowId);
		} catch (WorkflowException e) {
			return null;
		}
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
