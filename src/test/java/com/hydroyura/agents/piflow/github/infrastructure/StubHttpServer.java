package com.hydroyura.agents.piflow.github.infrastructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpServer;

/**
 * Локальный mock HTTP-сервер для тестов GitHub-адаптера без обращения к сети.
 */
class StubHttpServer implements AutoCloseable {

	record StubResponse(int status, String body, Map<String, String> headers, long delayMillis) {

		static StubResponse json(int status, String body) {
			return new StubResponse(status, body, Map.of(), 0);
		}

		static StubResponse json(int status, String body, Map<String, String> headers) {
			return new StubResponse(status, body, headers, 0);
		}

		static StubResponse delayed(int status, String body, long delayMillis) {
			return new StubResponse(status, body, Map.of(), delayMillis);
		}
	}

	record RecordedRequest(String method, String path, String query, String authorization, String body) {
	}

	private final HttpServer server;
	private final Map<String, List<StubResponse>> responses = new ConcurrentHashMap<>();
	private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

	private StubHttpServer(HttpServer server) {
		this.server = server;
	}

	static StubHttpServer start() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
			StubHttpServer stub = new StubHttpServer(server);
			server.createContext("/", stub::handle);
			server.start();
			return stub;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot start stub HTTP server", e);
		}
	}

	void stub(String method, String path, StubResponse... responseList) {
		responses.put(method + " " + path, new CopyOnWriteArrayList<>(responseList));
	}

	List<RecordedRequest> requests() {
		return List.copyOf(requests);
	}

	String baseUrl() {
		return "http://localhost:" + server.getAddress().getPort();
	}

	private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod();
		String path = exchange.getRequestURI().getPath();
		String query = exchange.getRequestURI().getRawQuery();
		String authorization = exchange.getRequestHeaders().getFirst("Authorization");
		String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

		requests.add(new RecordedRequest(method, path, query, authorization, body));

		List<StubResponse> queue = responses.get(method + " " + path);
		StubResponse response = resolve(queue, method, path);

		if (response.delayMillis() > 0) {
			try {
				Thread.sleep(response.delayMillis());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
		if (bytes.length == 0) {
			exchange.sendResponseHeaders(response.status(), -1);
		} else {
			exchange.sendResponseHeaders(response.status(), bytes.length);
			try (var out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		}
		exchange.close();
	}

	private StubResponse resolve(List<StubResponse> queue, String method, String path) {
		if (queue == null || queue.isEmpty()) {
			return StubResponse.json(404, "{\"message\":\"not stubbed: " + method + " " + path + "\"}");
		}
		if (queue.size() > 1) {
			return queue.remove(0);
		}
		return queue.get(0);
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
