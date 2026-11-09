package net.zaminmc.zea.share;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the paste client against a local mock server — no test ever
 * touches paste.helpcha.at. The mock is intentionally dumb: handlers
 * are installed per test through a mutable dispatcher.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class PasteServiceTest {
	private final Executor executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "paste-test");
		thread.setDaemon(true);
		return thread;
	});

	private final MockServer server = new MockServer();
	private final PasteService service = new PasteService(server.baseUri(), executor);

	@AfterEach
	void tearDown() {
		server.stop();
	}

	private static ShareException awaitFailure(CompletableFuture<?> future) {
		ExecutionException e = assertThrows(ExecutionException.class, future::get);
		return assertInstanceOf(ShareException.class, e.getCause());
	}

	@Test
	void uploadsReturnTheKeyFromTheResponseJson() throws Exception {
		server.respond(200, "{\"key\":\"wozajojele\"}");

		PasteKey key = service.upload("{}").get(5, TimeUnit.SECONDS);

		assertEquals("wozajojele", key.key());
		assertEquals("POST", server.lastMethod());
		assertEquals("/documents", server.lastPath());
		assertEquals("text/plain; charset=utf-8", server.lastHeader("Content-Type"));
		assertEquals("{}", server.lastBody());
	}

	@Test
	void aResponseWithoutAKeyFails() {
		server.respond(200, "{\"notakey\":true}");

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("could not read"), e.getMessage());
	}

	@Test
	void aNonJsonResponseFails() {
		server.respond(200, "OK!");

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("could not read"), e.getMessage());
	}

	@Test
	void anEmptyJsonResponseFails() {
		server.respond(200, "");

		awaitFailure(service.upload("{}"));
	}

	@Test
	void aMalformedKeyInTheResponseFails() {
		server.respond(200, "{\"key\":\"not a key\"}");

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("malformed key"), e.getMessage());
	}

	@Test
	void httpErrorsFailTheUpload() {
		server.respond(503, "busy");

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("503"), e.getMessage());
	}

	@Test
	void downloadsFetchTheRawEndpoint() throws Exception {
		server.respond(200, "{\"format\":\"zea-share\"}");

		String document = service.download(new PasteKey("wozajojele")).get(5, TimeUnit.SECONDS);

		assertEquals("{\"format\":\"zea-share\"}", document);
		assertEquals("GET", server.lastMethod());
		assertEquals("/raw/wozajojele", server.lastPath());
	}

	@Test
	void httpErrorsFailTheDownload() {
		server.respond(404, "gone");

		ShareException e = awaitFailure(service.download(new PasteKey("nope")));

		assertTrue(e.getMessage().contains("404"), e.getMessage());
	}

	@Test
	void oversizedDocumentsAreRejected() {
		server.respond(200, "x".repeat(PasteService.MAX_DOCUMENT_BYTES + 1));

		ShareException e = awaitFailure(service.download(new PasteKey("big")));

		assertTrue(e.getMessage().contains("larger"), e.getMessage());
	}

	@Test
	void documentsAtTheCapPass() throws Exception {
		server.respond(200, "x".repeat(PasteService.MAX_DOCUMENT_BYTES));

		String document = service.download(new PasteKey("edge")).get(5, TimeUnit.SECONDS);

		assertEquals(PasteService.MAX_DOCUMENT_BYTES, document.length());
	}

	@Test
	void redirectsAreNotFollowedAndFail() {
		server.redirect("https://evil.example/documents");

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("rejected"), e.getMessage());
	}

	@Test
	void aConnectionFailureSurfacesAsAReadableError() {
		server.stop(); // nothing is listening anymore

		ShareException e = awaitFailure(service.upload("{}"));

		assertTrue(e.getMessage().contains("Could not reach"), e.getMessage());
	}

	@Test
	void anUnreachableHostTimesOutOrRefusesQuickly() {
		// 203.0.113.0/24 is reserved for documentation; connections
		// to .1 hang until the connect timeout fires.
		PasteService unreachable = new PasteService(URI.create("https://203.0.113.1"), executor);

		ShareException e = awaitFailure(unreachable.upload("{}"));

		assertTrue(e.getMessage().contains("Could not reach"), e.getMessage());
	}

	@Test
	void aBusyExecutorRejectsWithoutBlocking() {
		AtomicInteger submissions = new AtomicInteger();
		Executor busy = task -> {
			if (submissions.incrementAndGet() == 1) {
				throw new java.util.concurrent.RejectedExecutionException("full");
			}
			task.run();
		};
		PasteService busyService = new PasteService(server.baseUri(), busy);
		server.respond(200, "{\"key\":\"abc\"}");

		ShareException rejected = awaitFailure(busyService.upload("{}"));

		assertTrue(rejected.getMessage().contains("still running"), rejected.getMessage());
	}

	@Test
	void closeWhileIdleIsHarmless() throws Exception {
		server.respond(200, "{\"key\":\"abc\"}");
		service.upload("{}").get(5, TimeUnit.SECONDS);

		service.close();
	}

	/** A minimal scripted HTTP server; each response is used once. */
	private static final class MockServer {
		private final HttpServer http;
		private volatile Responder responder = exchange -> send(exchange, 500, "not scripted");
		private volatile String lastMethod;
		private volatile String lastPath;
		private volatile String lastBody;
		private final java.util.Map<String, String> lastHeaders = new java.util.concurrent.ConcurrentHashMap<>();

		interface Responder {
			void respond(HttpExchange exchange) throws IOException;
		}

		MockServer() {
			try {
				http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			http.createContext("/", exchange -> {
				lastMethod = exchange.getRequestMethod();
				lastPath = exchange.getRequestURI().getPath();
				lastBody = new String(exchange.getRequestBody().readAllBytes(),
						StandardCharsets.UTF_8);
				lastHeaders.clear();
				exchange.getRequestHeaders().forEach((name, values) ->
						lastHeaders.put(name.toLowerCase(), String.join(",", values)));
				responder.respond(exchange);
				exchange.close();
			});
			http.start();
		}

		void respond(int status, String body) {
			responder = exchange -> send(exchange, status, body);
		}

		void redirect(String to) {
			responder = exchange -> {
				exchange.getResponseHeaders().add("Location", to);
				send(exchange, 302, "");
			};
		}

		private static void send(HttpExchange exchange, int status, String body) throws IOException {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
			if (bytes.length > 0) {
				try (OutputStream out = exchange.getResponseBody()) {
					out.write(bytes);
				}
			}
		}

		URI baseUri() {
			return URI.create("http://127.0.0.1:" + http.getAddress().getPort());
		}

		void stop() {
			http.stop(0);
		}

		String lastMethod() {
			return lastMethod;
		}

		String lastPath() {
			return lastPath;
		}

		String lastBody() {
			return lastBody;
		}

		String lastHeader(String name) {
			return lastHeaders.getOrDefault(name.toLowerCase(), "");
		}
	}
}
