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
}
