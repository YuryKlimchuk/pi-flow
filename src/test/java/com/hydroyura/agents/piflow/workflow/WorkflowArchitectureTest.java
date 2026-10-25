package com.hydroyura.agents.piflow.workflow;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Проверяет изоляцию домена и API модуля workflow от Temporal, Spring и
 * инфраструктурных пакетов.
 *
 * <p>Правила применяются только к production-коду (test-классы исключены).</p>
 */
class WorkflowArchitectureTest {

	private static final String DOMAIN = "com.hydroyura.agents.piflow.workflow.domain..";
	private static final String API = "com.hydroyura.agents.piflow.workflow.api..";
	private static final String INFRASTRUCTURE = "com.hydroyura.agents.piflow.workflow.infrastructure..";

	private final JavaClasses classes = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.hydroyura.agents.piflow");

	@Test
	void domainDoesNotDependOnTemporal() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAnyPackage("io.temporal..")
				.check(classes);
	}

	@Test
	void apiDoesNotDependOnTemporal() {
		noClasses().that().resideInAPackage(API)
				.should().dependOnClassesThat().resideInAnyPackage("io.temporal..")
				.check(classes);
	}

	@Test
	void domainDoesNotDependOnSpring() {
		noClasses().that().resideInAPackage(DOMAIN)
				.should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
				.check(classes);
	}

	@Test
	void apiDoesNotDependOnSpring() {
		noClasses().that().resideInAPackage(API)
				.and().doNotHaveSimpleName("package-info")
				.should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
				.check(classes);
	}

	@Test
	void domainAndApiDoNotDependOnInfrastructure() {
		noClasses().that().resideInAnyPackage(DOMAIN, API)
				.should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
				.check(classes);
	}

	@Test
	void onlyInfrastructureMayDependOnTemporal() {
		classes().that().resideOutsideOfPackage(INFRASTRUCTURE)
				.should().onlyDependOnClassesThat().resideOutsideOfPackage("io.temporal..")
				.check(classes);
	}
}
