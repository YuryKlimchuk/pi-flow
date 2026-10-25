package com.hydroyura.agents.piflow.agent;

import java.nio.charset.StandardCharsets;

/**
 * Тестовый исполняемый процесс, имитирующий Pi CLI для process-тестов модуля agent.
 *
 * <p>Поддерживает режимы через аргументы: {@code --exit-code=N}, {@code --hang},
 * {@code --big}. Читает stdin и пишет его и окружение в stdout.</p>
 */
public final class FakePi {

	private FakePi() {
	}

	public static void main(String[] args) throws Exception {
		int exitCode = 0;
		boolean hang = false;
		boolean big = false;
		for (String arg : args) {
			if (arg.startsWith("--exit-code=")) {
				exitCode = Integer.parseInt(arg.substring("--exit-code=".length()));
			} else if (arg.startsWith("--hang")) {
				hang = true;
			} else if (arg.startsWith("--big")) {
				big = true;
			}
		}

		String stdin = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);

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
			System.out.println("STDIN:" + stdin);
			System.out.println("ARGS:" + String.join("|", args));
			System.out.println("ENV_SECRET:" + System.getenv("SECRET_VAR"));
			System.out.println("ENV_OTHER:" + System.getenv("OTHER"));
			System.err.println("STDERR:done");
		}

		System.out.flush();
		System.err.flush();
		System.exit(exitCode);
	}
}
