package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.github.api.BranchOperations;
import com.hydroyura.agents.piflow.github.api.PullRequestOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceGitOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflowImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowAnalysisActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowCleanupActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowDesignActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowDeveloperActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowFinalizationActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowReviewerActivitiesImpl;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Собирает Temporal client и worker из typed-конфигурации.
 */
@Configuration
public class TemporalConfiguration {

	@Bean
	WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
		return WorkflowServiceStubs.newServiceStubs(
				WorkflowServiceStubsOptions.newBuilder()
						.setTarget(properties.endpoint())
						.build());
	}

	@Bean
	WorkflowClient workflowClient(WorkflowServiceStubs stubs, TemporalProperties properties) {
		return WorkflowClient.newInstance(
				stubs,
				WorkflowClientOptions.newBuilder()
						.setNamespace(properties.namespace())
						.build());
	}

	@Bean
	WorkerFactory workerFactory(WorkflowClient workflowClient, TemporalProperties properties,
			WorkspaceOperations workspaceOperations, WorkspaceGitOperations workspaceGitOperations,
			WorkspaceLockOperations workspaceLockOperations, AgentOperations agentOperations,
			BranchOperations branchOperations, PullRequestOperations pullRequestOperations) {
		WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
		Worker worker = factory.newWorker(properties.taskQueue());
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(
				new WorkflowPreparationActivitiesImpl(workspaceOperations, workspaceLockOperations),
				new WorkflowAnalysisActivitiesImpl(agentOperations),
				new WorkflowDesignActivitiesImpl(agentOperations),
				new WorkflowDeveloperActivitiesImpl(agentOperations),
				new WorkflowReviewerActivitiesImpl(agentOperations),
				new WorkflowFinalizationActivitiesImpl(workspaceOperations, workspaceGitOperations,
						branchOperations, pullRequestOperations),
				new WorkflowCleanupActivitiesImpl(workspaceLockOperations));
		factory.start();
		return factory;
	}
}
