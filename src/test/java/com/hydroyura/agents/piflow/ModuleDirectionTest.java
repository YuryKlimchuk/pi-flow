package com.hydroyura.agents.piflow;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/**
 * Проверяет направление зависимостей между модулями и изоляцию инфраструктуры
 * модуля github.
 */
class ModuleDirectionTest {

	private static final String GITHUB = "com.hydroyura.agents.piflow.github..";
	private static final String GITHUB_INFRASTRUCTURE = "com.hydroyura.agents.piflow.github.infrastructure..";
	private static final String AGENT = "com.hydroyura.agents.piflow.agent..";
	private static final String AGENT_INFRASTRUCTURE = "com.hydroyura.agents.piflow.agent.infrastructure..";

	private final JavaClasses classes = new ClassFileImporter().importPackages("com.hydroyura.agents.piflow");

	@Test
	void githubInfrastructureIsNotLeakedToOtherModules() {
		noClasses()
				.that().resideOutsideOfPackage(GITHUB)
				.should().dependOnClassesThat().resideInAPackage(GITHUB_INFRASTRUCTURE)
				.check(classes);
	}

	@Test
	void workflowDoesNotDependOnWeb() {
		noClasses()
				.that().resideInAPackage("com.hydroyura.agents.piflow.workflow..")
				.should().dependOnClassesThat().resideInAPackage("com.hydroyura.agents.piflow.web..")
				.check(classes);
	}

	@Test
	void webDoesNotDependOnGithubInfrastructure() {
		noClasses()
				.that().resideInAPackage("com.hydroyura.agents.piflow.web..")
				.should().dependOnClassesThat().resideInAPackage(GITHUB_INFRASTRUCTURE)
				.check(classes);
	}

	@Test
	void agentInfrastructureIsNotLeakedToOtherModules() {
		noClasses()
				.that().resideOutsideOfPackage(AGENT)
				.should().dependOnClassesThat().resideInAPackage(AGENT_INFRASTRUCTURE)
				.check(classes);
	}

	@Test
	void agentIsLeafModule() {
		noClasses()
				.that().resideInAPackage(AGENT)
				.should().dependOnClassesThat().resideInAnyPackage(
						"com.hydroyura.agents.piflow.workflow..",
						"com.hydroyura.agents.piflow.workspace..",
						"com.hydroyura.agents.piflow.github..",
						"com.hydroyura.agents.piflow.interaction..",
						"com.hydroyura.agents.piflow.web..")
				.check(classes);
	}
}
