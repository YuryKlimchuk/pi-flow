package com.hydroyura.agents.piflow.workspace.infrastructure;

import java.nio.file.Path;

import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import org.springframework.stereotype.Component;

/**
 * Размещает рабочие копии и блокировки на основе typed-конфигурации.
 */
@Component
public class PropertiesWorkspaceLayout implements WorkspaceLayout {

	private final Path baseDirectory;
	private final Path lockDirectory;

	public PropertiesWorkspaceLayout(WorkspaceProperties properties) {
		this.baseDirectory = resolve(properties.baseDirectory(), ".pi-flow", "workspaces");
		this.lockDirectory = resolve(properties.lockDirectory(), ".pi-flow", "locks");
	}

	@Override
	public String repositoryKey(String repositoryUrl) {
		return canonicalKey(repositoryUrl);
	}

	@Override
	public Path baseDirectory() {
		return baseDirectory;
	}

	@Override
	public Path repositoryDirectory(String repositoryUrl) {
		return baseDirectory.resolve(canonicalKey(repositoryUrl));
	}

	@Override
	public Path lockDirectory() {
		return lockDirectory;
	}

	private static Path resolve(Path configured, String... defaultSegments) {
		if (configured != null) {
			return configured.toAbsolutePath().normalize();
		}
		Path result = Path.of(System.getProperty("user.home"));
		for (String segment : defaultSegments) {
			result = result.resolve(segment);
		}
		return result;
	}

	private static String canonicalKey(String repositoryUrl) {
		String normalized = repositoryUrl.trim();
		normalized = normalized.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
		int at = normalized.indexOf('@');
		if (at >= 0 && normalized.startsWith("git")) {
			normalized = normalized.substring(at + 1);
		}
		normalized = normalized.replaceFirst("\\.git$", "");
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		normalized = normalized.toLowerCase();

		StringBuilder sb = new StringBuilder();
		for (char c : normalized.toCharArray()) {
			if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
				sb.append(c);
			} else {
				sb.append('-');
			}
		}
		String key = sb.toString().replaceAll("-{2,}", "-").replaceAll("^-+", "").replaceAll("-+$", "");
		return key.isEmpty() ? "repo" : key;
	}
}
