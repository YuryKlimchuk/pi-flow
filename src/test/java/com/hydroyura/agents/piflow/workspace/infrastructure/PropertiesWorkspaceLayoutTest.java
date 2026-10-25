package com.hydroyura.agents.piflow.workspace.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesWorkspaceLayoutTest {

	@TempDir
	Path tempDir;

	private PropertiesWorkspaceLayout layout;

	@BeforeEach
	void setUp() {
		WorkspaceProperties properties = new WorkspaceProperties(tempDir.resolve("ws"), tempDir.resolve("locks"),
				Duration.ofMinutes(1));
		layout = new PropertiesWorkspaceLayout(properties);
	}

	@Test
	void repositoryKey_whenHttpsUrl_shouldNormalize() {
		assertThat(layout.repositoryKey("https://github.com/acme/repo.git")).isEqualTo("github.com-acme-repo");
	}

	@Test
	void repositoryKey_whenScpLike_shouldNormalize() {
		assertThat(layout.repositoryKey("git@github.com:acme/repo.git")).isEqualTo("github.com-acme-repo");
	}

	@Test
	void repositoryDirectory_whenSameRepository_shouldBeSamePathUnderBase() {
		Path first = layout.repositoryDirectory("https://github.com/acme/repo.git");
		Path second = layout.repositoryDirectory("https://github.com/acme/repo.git");

		assertThat(first).isEqualTo(second);
		assertThat(first.normalize().toString()).startsWith(layout.baseDirectory().normalize().toString());
	}

	@Test
	void repositoryDirectory_whenTraversalAttempt_shouldStayUnderBase() {
		Path dir = layout.repositoryDirectory("https://host/../../etc");

		assertThat(dir.normalize().toString()).startsWith(layout.baseDirectory().normalize().toString());
	}
}
