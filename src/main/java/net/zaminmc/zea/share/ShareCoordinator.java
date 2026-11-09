package net.zaminmc.zea.share;

import net.zaminmc.zea.buttons.InventoryButton;
import net.zaminmc.zea.buttons.InventoryButtons;
import net.zaminmc.zea.config.ZeaConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Coordinates sharing: exports serialize the live state, validate it
 * round-trip and upload; imports download, fully validate and hold the
 * result as a pending import that nothing applies until the player
 * confirms.
 *
 * <p>Threading: {@code export} and {@code beginImport} may be called on
 * the client thread only; they return futures that complete on the
 * injected client-thread executor, so feedback can touch the UI
 * directly. {@code confirmImport} and {@code cancelImport} are
 * synchronous, client-thread operations. At most one import is pending;
 * starting a new one discards the old without applying it.
 */
public final class ShareCoordinator {
        private final PasteTransport transport;
        private final InventoryButtons buttons;
        private final ZeaConfig config;
        private final Executor clientThread;

        private PendingImport pending;

        /** A successful export: the paste and an optional note about what was left out. */
        public record Export(PasteKey key, String note) {
        }

        /** A validated import waiting for the player's confirmation. */
        public record ImportPreview(ShareType type, String summary, String application) {
        }

        /** The result of a confirmed import. */
        public record Applied(ShareType type) {
        }

        private record PendingImport(ShareType type, SharePayload payload) {
        }

        public ShareCoordinator(PasteTransport transport, InventoryButtons buttons, ZeaConfig config,
                        Executor clientThread) {
                this.transport = transport;
                this.buttons = buttons;
                this.config = config;
                this.clientThread = clientThread;
        }

        /**
         * Serializes the current state of one category, re-validates the
         * document, and uploads it.
         */
        public CompletableFuture<Export> export(ShareType type) {
                int[] skipped = new int[1];
                ShareDocument document = documentFor(type, skipped);
                // Round-trip: an export that cannot be imported back is a bug,
                // and uploading one would hand every recipient a broken share.
                try {
                        ShareDocument.parse(document.toJson());
                } catch (ShareException e) {
                        return CompletableFuture.failedFuture(e);
                }
                String note = type == ShareType.INVENTORY_BUTTONS && skipped[0] > 0
                                ? skipped[0] + (skipped[0] == 1 ? " unfinished button was" : " unfinished buttons were")
                                                + " left out"
                                : "";
                return transport.upload(document.toJson())
                                .thenApplyAsync(key -> new Export(key, note), clientThread);
        }

        /**
         * Downloads and validates a paste, then holds it as the pending
         * import. Any failure leaves no pending import behind.
         */
        public CompletableFuture<ImportPreview> beginImport(String input) {
                PasteKey key;
                try {
                        key = PasteKey.parse(input);
                } catch (ShareException e) {
                        return CompletableFuture.failedFuture(e);
                }
                return transport.download(key)
                                .thenApplyAsync(body -> holdValidated(body), clientThread);
        }

        /**
         * Applies the pending import. Buttons replace the whole set;
         * general settings merge field by field.
         *
         * @return what was applied, or {@code null} when nothing is pending
         */
        public Applied confirmImport() {
                PendingImport held = pending;
                if (held == null) {
                        return null;
                }
                pending = null;
                if (held.payload instanceof InventoryButtonsPayload buttonPayload) {
                        buttons.applyImport(buttonPayload.buttons());
                } else if (held.payload instanceof GeneralConfigPayload generalPayload) {
                        generalPayload.applyTo(config);
                        config.saveNow();
                }
                return new Applied(held.type());
        }

        /** Discards the pending import, if any. */
        public void cancelImport() {
                pending = null;
        }

        /** Whether an import is waiting for confirmation. */
        public boolean hasPendingImport() {
                return pending != null;
        }

        private ImportPreview holdValidated(String body) {
                try {
                        ShareDocument document = ShareDocument.parse(body);
                        PendingImport held = new PendingImport(document.type(), decode(document));
                        pending = held;
                        return new ImportPreview(document.type(), held.payload().summary(),
                                        applicationFor(document.type()));
                } catch (ShareException e) {
                        throw new CompletionException(e);
                }
        }

        private SharePayload decode(ShareDocument document) throws ShareException {
                return switch (document.type()) {
                        case INVENTORY_BUTTONS -> InventoryButtonsPayload.fromJson(document.data());
                        case GENERAL -> GeneralConfigPayload.fromJson(document.data());
                };
        }

        private ShareDocument documentFor(ShareType type, int[] skipped) {
                return switch (type) {
                        case INVENTORY_BUTTONS -> ShareDocument.of(type,
                                        InventoryButtonsPayload.of(buttons.buttons(), skipped).toJson());
                        case GENERAL -> ShareDocument.of(type, GeneralConfigPayload.of(config).toJson());
                };
        }

        private String applicationFor(ShareType type) {
                List<InventoryButton> current = buttons.buttons();
                return type == ShareType.INVENTORY_BUTTONS
                                ? "Replaces your " + current.size()
                                                + (current.size() == 1 ? " button" : " buttons")
                                : "Updates your settings; anything the share leaves out stays as it is";
        }
}
