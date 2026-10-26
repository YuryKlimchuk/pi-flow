package com.hydroyura.agents.piflow.agent.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;
import com.hydroyura.agents.piflow.agent.domain.RoleResources;

import org.springframework.stereotype.Component;

/**
 * Резолвит роль-ресурсы из classpath по конвенции {@code roles/<role>/}.
 *
 * <p>Для ресурсов, доступных как файлы (рабочее дерево, {@code target/classes}),
 * возвращает прямой путь. Файл системного промпта из упакованного jar
 * материализуется во временный файл; каталог скилов из jar на первом этапе
 * не поддерживается и завершается ошибкой запуска.</p>
 */
@Component
public class ClasspathRoleResources implements RoleResources {

	private static final String ROOT = "roles/";

	@Override
	public Path systemPromptFile(String role) {
		String resourcePath = ROOT + normalize(role) + "/system-prompt.md";
		URL url = requireResource(resourcePath);
		if ("file".equals(url.getProtocol())) {
			return asPath(url, resourcePath);
		}
		return materializeFile(url, resourcePath);
	}

	@Override
	public Path skillsDirectory(String role) {
		String resourcePath = ROOT + normalize(role) + "/skills/";
		URL url = requireResource(resourcePath);
		if (!"file".equals(url.getProtocol())) {
			throw AgentProcessException.launch(
					"role skills directory inside a packaged archive is not supported yet: " + resourcePath, null);
		}
		return asPath(url, resourcePath);
	}

	private URL requireResource(String resourcePath) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader == null) {
			loader = ClasspathRoleResources.class.getClassLoader();
		}
		URL url = loader.getResource(resourcePath);
		if (url == null) {
			throw AgentProcessException.launch("role resource not found: " + resourcePath, null);
		}
		return url;
	}

	private Path asPath(URL url, String resourcePath) {
		try {
			return Path.of(url.toURI());
		} catch (URISyntaxException e) {
			throw AgentProcessException.launch("invalid role resource URI: " + resourcePath, e);
		}
	}

	private Path materializeFile(URL url, String resourcePath) {
		try (InputStream in = url.openStream()) {
			Path temp = Files.createTempFile("pi-flow-role-", ".md");
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			return temp;
		} catch (IOException e) {
			throw AgentProcessException.launch("failed to materialize role resource: " + resourcePath, e);
		}
	}

	private String normalize(String role) {
		if (role == null || role.isBlank()) {
			throw AgentProcessException.launch("role must not be blank", null);
		}
		return role.toLowerCase(Locale.ROOT);
	}
}
