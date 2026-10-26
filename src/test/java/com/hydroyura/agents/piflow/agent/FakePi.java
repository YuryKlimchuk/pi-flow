package com.hydroyura.agents.piflow.agent;

/**
 * Тестовый исполняемый процесс, имитирующий Pi CLI в print-режиме для
 * process-тестов модуля agent.
 *
 * <p>Читает реальные аргументы Pi ({@code --append-system-prompt <file>},
 * {@code --skill <dir>}, {@code --} и первый промпт) и управляется через
 * переменные окружения: {@code FAKE_PI_EXIT_CODE}, {@code FAKE_PI_HANG},
 * {@code FAKE_PI_BIG}.</p>
 */
public final class FakePi {

	private FakePi() {
	}

	public static void main(String[] args) throws Exception {
		boolean hang = "true".equals(System.getenv("FAKE_PI_HANG"));
		boolean big = "true".equals(System.getenv("FAKE_PI_BIG"));
		int exitCode = Integer.parseInt(envOrDefault("FAKE_PI_EXIT_CODE", "0"));

		String systemPrompt = null;
		String skill = null;
		StringBuilder message = new StringBuilder();
		boolean afterDoubleDash = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (!afterDoubleDash && "--".equals(arg)) {
				afterDoubleDash = true;
				continue;
			}
			if (!afterDoubleDash && "--append-system-prompt".equals(arg)) {
				systemPrompt = args[++i];
				continue;
			}
			if (!afterDoubleDash && "--skill".equals(arg)) {
				skill = args[++i];
				continue;
			}
			if (afterDoubleDash) {
				if (!message.isEmpty()) {
					message.append(' ');
				}
				message.append(arg);
			}
		}

		if (hang) {
			Thread.sleep(120_000);
			return;
		}

		if (big) {
			String chunk = "x".repeat(1024);
			for (int i = 0; i < 512; i++) {
				System.out.print(chunk);
				System.err.print(chunk);
			}
		} else {
			System.out.println("ARGS:" + String.join("|", args));
			System.out.println("SYSTEM_PROMPT:" + systemPrompt);
			System.out.println("SKILL:" + skill);
			System.out.println("MESSAGE:" + message);
			System.out.println("ENV_SECRET:" + System.getenv("SECRET_VAR"));
			System.out.println("ENV_OTHER:" + System.getenv("OTHER"));
			System.err.println("STDERR:done");
		}

		System.out.flush();
		System.err.flush();
		System.exit(exitCode);
	}

	private static String envOrDefault(String name, String defaultValue) {
		String value = System.getenv(name);
		return value == null ? defaultValue : value;
	}
}
