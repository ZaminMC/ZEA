package net.zaminmc.zea.share;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * The HelpChat paste client: upload a document, download a raw document.
 *
 * <p>All IO runs on the injected executor (bounded, never the client
 * thread); the returned futures complete there, and callers marshal to
 * the client thread themselves. Redirects are never followed — the
 * service's endpoints answer directly, so a redirect is treated as a
 * failure rather than silently followed to another host. Success is
 * never assumed from a connection: status codes, response JSON shape
 * and key validity are all checked.
 *
 * <p>The base URI is injectable so tests run against a local mock; the
 * production wiring always uses {@link PasteKey#BASE_URL}, whose host
 * the import path additionally pins through {@link PasteKey}.
 */
public final class PasteService implements PasteTransport {
	/** Hard cap on downloaded documents; generous for JSON, small for a heap. */
	public static final int MAX_DOCUMENT_BYTES = 1024 * 1024;

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private final URI baseUri;
	private final Executor executor;
	private final HttpClient client;

	public PasteService(URI baseUri, Executor executor) {
		this.baseUri = baseUri;
		this.executor = executor;
		this.client = HttpClient.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT)
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	/**
	 * Uploads a document and returns its key.
	 *
	 * @throws ShareException via the future when the upload fails,
	 *                         the response is malformed, or the key is invalid
	 */
	public CompletableFuture<PasteKey> upload(String document) {
		return submit(() -> {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(baseUri.resolve("/documents"))
					.timeout(REQUEST_TIMEOUT)
					.header("Content-Type", "text/plain; charset=utf-8")
					.POST(HttpRequest.BodyPublishers.ofString(document))
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new ShareException("The paste service rejected the upload (HTTP "
						+ response.statusCode() + ")");
			}
			return keyFromBody(response.body());
		});
	}

	/**
	 * Downloads a raw document, enforcing the size cap.
	 *
	 * @throws ShareException via the future on any failure, including
	 *                         documents larger than {@link #MAX_DOCUMENT_BYTES}
	 */
	public CompletableFuture<String> download(PasteKey key) {
		return submit(() -> {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(baseUri.resolve("/raw/" + key.key()))
					.timeout(REQUEST_TIMEOUT)
					.GET()
					.build();
			HttpResponse<InputStream> response = client.send(request,
					HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() / 100 != 2) {
				response.body().close();
				throw new ShareException("The paste could not be downloaded (HTTP "
						+ response.statusCode() + ")");
			}
			return readCapped(response.body());
		});
	}

	/** Shuts the underlying client down; in-flight requests fail fast. */
	public void close() {
		client.close();
	}

	private PasteKey keyFromBody(String body) throws ShareException {
		try {
			String key = JsonParser.parseString(body).getAsJsonObject().get("key").getAsString();
			if (!PasteKey.isValidKey(key)) {
				throw new ShareException("The paste service returned a malformed key");
			}
			return new PasteKey(key);
		} catch (JsonParseException | IllegalStateException | NullPointerException e) {
			throw new ShareException("The paste service answered with something ZEA "
					+ "could not read", e);
		}
	}

	/** Reads up to the cap plus one byte, so an over-sized body is detectable. */
	private String readCapped(InputStream body) throws ShareException {
		try (body) {
			java.io.ByteArrayOutputStream buffered = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = body.read(buffer)) != -1) {
				if (buffered.size() + read > MAX_DOCUMENT_BYTES) {
					throw new ShareException("That paste is larger than ZEA imports "
							+ "(over " + (MAX_DOCUMENT_BYTES / 1024) + " KB)");
				}
				buffered.write(buffer, 0, read);
			}
			// Decode once, after all bytes are in — chunk-wise decoding
			// could split a multi-byte character across reads.
			return buffered.toString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new ShareException("The download was interrupted", e);
		}
	}

	private <T> CompletableFuture<T> submit(IoTask<T> task) {
		CompletableFuture<T> result = new CompletableFuture<>();
		try {
			executor.execute(() -> {
				try {
					result.complete(task.run());
				} catch (ShareException e) {
					result.completeExceptionally(e);
				} catch (IOException | InterruptedException e) {
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					result.completeExceptionally(new ShareException(
							"Could not reach the paste service", e));
				} catch (RuntimeException e) {
					result.completeExceptionally(e);
				} catch (Exception e) {
					result.completeExceptionally(new ShareException(
							"The share operation failed unexpectedly", e));
				}
			});
		} catch (RejectedExecutionException e) {
			result.completeExceptionally(new ShareException(
					"Another share operation is still running", e));
		}
		return result;
	}

	@FunctionalInterface
	private interface IoTask<T> {
		T run() throws Exception;
	}
}
