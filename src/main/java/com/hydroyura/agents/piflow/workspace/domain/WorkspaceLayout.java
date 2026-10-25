package com.hydroyura.agents.piflow.workspace.domain;

import java.nio.file.Path;

/**
 * Внутренний порт размещения рабочих копий.
 */
public interface WorkspaceLayout {

	String repositoryKey(String repositoryUrl);

	Path baseDirectory();

	Path repositoryDirectory(String repositoryUrl);

	Path lockDirectory();
}
