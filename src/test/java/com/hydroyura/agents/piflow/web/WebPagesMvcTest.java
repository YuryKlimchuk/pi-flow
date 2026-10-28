package com.hydroyura.agents.piflow.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.IssueOperations;
import com.hydroyura.agents.piflow.github.api.IssuePage;
import com.hydroyura.agents.piflow.github.api.IssueState;
import com.hydroyura.agents.piflow.github.api.IssueSummary;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.api.RepositoryOperations;
import com.hydroyura.agents.piflow.github.api.RepositoryPage;
import com.hydroyura.agents.piflow.github.api.RepositorySummary;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {RepositoryController.class, IssueController.class, WelcomeController.class, WorkflowController.class})
class WebPagesMvcTest {

	private static final String SENTINEL_PAT = "SENTINEL_PAT_12345";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RepositoryOperations repositoryOperations;

	@MockitoBean
	private IssueOperations issueOperations;

	@MockitoBean
	private WorkflowOperations workflowOperations;

	@Test
	void welcomePage_shouldRenderIndex() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(content().string(containsString("pi-flow")))
				.andExpect(content().string(containsString("Репозитории")));
	}

	@Test
	void repositoriesPage_whenHasData_shouldRenderOwnerAndName() throws Exception {
		RepositoryPage page = new RepositoryPage(
				List.of(new RepositorySummary(1L, "acme", "repo", "https://github.com/acme/repo")), 1, 30, false);
		when(repositoryOperations.listRepositories(any(PageRequest.class))).thenReturn(page);

		mockMvc.perform(get("/repositories"))
				.andExpect(status().isOk())
				.andExpect(view().name("repositories"))
				.andExpect(content().string(containsString("acme")))
				.andExpect(content().string(containsString("repo")));
	}

	@Test
	void repositoriesPage_whenEmpty_shouldShowEmptyMessage() throws Exception {
		when(repositoryOperations.listRepositories(any(PageRequest.class)))
				.thenReturn(new RepositoryPage(List.of(), 1, 30, false));

		mockMvc.perform(get("/repositories"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Нет доступных репозиториев")));
	}

	@Test
	void repositoriesPage_whenHasNext_shouldRenderNextLink() throws Exception {
		RepositoryPage page = new RepositoryPage(
				List.of(new RepositorySummary(1L, "acme", "repo", "https://github.com/acme/repo")), 1, 30, true);
		when(repositoryOperations.listRepositories(any(PageRequest.class))).thenReturn(page);

		mockMvc.perform(get("/repositories"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("page=2")));
	}

	@Test
	void repositoriesPage_whenAuthenticationError_shouldNotLeakPat() throws Exception {
		GithubException error = new GithubException("listRepositories", GithubException.ErrorType.AUTHENTICATION, 401,
				false, "leaked " + SENTINEL_PAT);
		when(repositoryOperations.listRepositories(any(PageRequest.class))).thenThrow(error);

		mockMvc.perform(get("/repositories"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Personal Access Token")))
				.andExpect(content().string(not(containsString(SENTINEL_PAT))));
	}

	@Test
	void repositoriesPage_whenRateLimitError_shouldShowRetry() throws Exception {
		when(repositoryOperations.listRepositories(any(PageRequest.class)))
				.thenThrow(GithubException.rateLimit("listRepositories", 429));

		mockMvc.perform(get("/repositories"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("лимит")))
				.andExpect(content().string(containsString("Повторить")));
	}

	@Test
	void issuesPage_whenHasData_shouldRenderIssueFields() throws Exception {
		IssuePage page = new IssuePage(
				List.of(new IssueSummary(7L, "Bug", IssueState.OPEN, "alice", "https://github.com/acme/repo/issues/7",
						false)),
				1, 30, false);
		when(issueOperations.listIssues(any(), any(), any(), any())).thenReturn(page);

		mockMvc.perform(get("/repositories/acme/repo/issues"))
				.andExpect(status().isOk())
				.andExpect(view().name("issues"))
				.andExpect(content().string(containsString("acme")))
				.andExpect(content().string(containsString("Bug")))
				.andExpect(content().string(containsString("Открыт")));
	}

	@Test
	void issuesPage_whenEmpty_shouldShowEmptyMessage() throws Exception {
		when(issueOperations.listIssues(any(), any(), any(), any())).thenReturn(new IssuePage(List.of(), 1, 30, false));

		mockMvc.perform(get("/repositories/acme/repo/issues"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("нет Issues")));
	}

	@Test
	void issuesPage_whenNetworkError_shouldNotLeakPat() throws Exception {
		GithubException error = new GithubException("listIssues", GithubException.ErrorType.NETWORK, 0, true,
				"leaked " + SENTINEL_PAT, new RuntimeException());
		when(issueOperations.listIssues(any(), any(), any(), any())).thenThrow(error);

		mockMvc.perform(get("/repositories/acme/repo/issues"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("подключиться к GitHub")))
				.andExpect(content().string(not(containsString(SENTINEL_PAT))));
	}

	@Test
	void issuesPage_shouldRenderStartWorkflowForm() throws Exception {
		IssuePage page = new IssuePage(
				List.of(new IssueSummary(7L, "Bug", IssueState.OPEN, "alice", "https://github.com/acme/repo/issues/7",
						false)),
				1, 30, false);
		when(issueOperations.listIssues(any(), any(), any(), any())).thenReturn(page);

		mockMvc.perform(get("/repositories/acme/repo/issues"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/workflows/start")))
				.andExpect(content().string(containsString("Взять в работу")));
	}

	@Test
	void workflowStart_shouldRedirectToTrack() throws Exception {
		mockMvc.perform(post("/workflows/start")
					.param("owner", "acme")
					.param("repository", "demo")
					.param("issueNumber", "42")
					.param("workflowId", "wf-1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/workflows/track?workflowId=wf-1"));
	}

	@Test
	void workflowTrack_whenRunning_shouldShowIndicator() throws Exception {
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.RUNNING, WorkflowStage.ANALYZING_REQUIREMENTS, null, null, null));

		mockMvc.perform(get("/workflows/track").param("workflowId", "wf-1"))
				.andExpect(status().isOk())
				.andExpect(view().name("workflow-status"))
				.andExpect(content().string(containsString("Анализ задачи выполняется")));
	}

	@Test
	void workflowTrack_whenResult_shouldShowTextAndContinue() throws Exception {
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.RUNNING, WorkflowStage.ANALYZING_REQUIREMENTS, "Текст анализа", "feature/issue-42", null));

		mockMvc.perform(get("/workflows/track").param("workflowId", "wf-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Текст анализа")))
				.andExpect(content().string(containsString("feature/issue-42")))
				.andExpect(content().string(containsString("Продолжить")));
	}

	@Test
	void workflowTrack_whenFailed_shouldShowError() throws Exception {
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.FAILED, WorkflowStage.ANALYZING_REQUIREMENTS, null, null, "boom"));

		mockMvc.perform(get("/workflows/track").param("workflowId", "wf-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Workflow завершился ошибкой")))
				.andExpect(content().string(containsString("boom")));
	}

	@Test
	void workflowContinue_shouldCallContinueAndRedirect() throws Exception {
		mockMvc.perform(post("/workflows/continue")
					.param("workflowId", "wf-1")
					.param("branchName", "feature/issue-42"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/workflows/track?workflowId=wf-1"));

		verify(workflowOperations).continueWorkflow(any());
	}

	@Test
	void workflowRework_shouldCallReworkAndRedirect() throws Exception {
		mockMvc.perform(post("/workflows/rework")
					.param("workflowId", "wf-1")
					.param("feedback", "добавь риски"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/workflows/track?workflowId=wf-1"));

		verify(workflowOperations).rework(any());
	}

	@Test
	void workflowRework_whenBlankFeedback_shouldShowError() throws Exception {
		when(workflowOperations.rework(any())).thenThrow(WorkflowException.validation("feedback must not be blank"));
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.RUNNING, WorkflowStage.ANALYZING_REQUIREMENTS, "text", "feature/issue-42", null));

		mockMvc.perform(post("/workflows/rework")
					.param("workflowId", "wf-1")
					.param("feedback", "  "))
				.andExpect(status().isOk())
				.andExpect(view().name("workflow-status"))
				.andExpect(content().string(containsString("feedback must not be blank")));
	}

	@Test
	void workflowTrack_whenDesigningSolution_shouldShowArchitectProgress() throws Exception {
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.RUNNING, WorkflowStage.DESIGNING_SOLUTION, null, null, null));

		mockMvc.perform(get("/workflows/track").param("workflowId", "wf-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Проектирование решения выполняется")));
	}

	@Test
	void workflowTrack_whenWaitingForUser_shouldShowResultAndActions() throws Exception {
		when(workflowOperations.getState("wf-1")).thenReturn(
				state(WorkflowStatus.WAITING_FOR_USER, WorkflowStage.ANALYZING_REQUIREMENTS, "Текст анализа", "feature/issue-42", null));

		mockMvc.perform(get("/workflows/track").param("workflowId", "wf-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("ожидание вашего решения")))
				.andExpect(content().string(containsString("Исправить")));
	}

	private WorkflowState state(WorkflowStatus status, WorkflowStage stage, String analysisText, String branchName,
			String errorMessage) {
		return new WorkflowState("wf-1", "https://github.com/acme/demo.git", 42, status, stage, List.of(), "exec-1",
				analysisText, branchName, null, null, null, null, null, errorMessage, List.of(), null, null, 0, null);
	}
}
