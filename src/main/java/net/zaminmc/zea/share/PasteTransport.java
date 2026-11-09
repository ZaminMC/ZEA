package net.zaminmc.zea.share;

import java.util.concurrent.CompletableFuture;

/**
 * The paste transport the {@link ShareCoordinator} talks to — upload a
 * document, download one. {@link PasteService} is the HTTP
 * implementation; the seam exists so the coordinator's import state
 * machine is testable without a network stack.
 */
public interface PasteTransport {
        /** Uploads a document and completes with its key. */
        CompletableFuture<PasteKey> upload(String document);

        /** Downloads a raw document and completes with its text. */
        CompletableFuture<String> download(PasteKey key);
}
