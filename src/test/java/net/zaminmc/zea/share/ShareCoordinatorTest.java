package net.zaminmc.zea.share;

import net.zaminmc.zea.buttons.ButtonActivator;
import net.zaminmc.zea.buttons.ButtonAnchor;
import net.zaminmc.zea.buttons.InventoryButton;
import net.zaminmc.zea.buttons.InventoryButtons;
import net.zaminmc.zea.config.ZeaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareCoordinatorTest {
        @TempDir
        Path dir;

        /** Records uploads and serves downloads per key; completes inline. */
        private static final class FakeTransport implements PasteTransport {
                final List<String> uploads = new ArrayList<>();
                final Map<String, String> documents = new HashMap<>();

                @Override
                public CompletableFuture<PasteKey> upload(String document) {
                        uploads.add(document);
                        PasteKey key = new PasteKey("key" + uploads.size());
                        documents.put(key.key(), document);
                        return CompletableFuture.completedFuture(key);
                }

                @Override
                public CompletableFuture<String> download(PasteKey key) {
                        String document = documents.get(key.key());
                        return document == null
                                        ? CompletableFuture.failedFuture(new ShareException("That paste does not exist"))
                                        : CompletableFuture.completedFuture(document);
                }
        }

        private static InventoryButton button(String name, String command) {
                return new InventoryButton(UUID.randomUUID(), name, command, "minecraft:compass",
                                ButtonAnchor.TOP_RIGHT, 0, 0, true);
        }

        private InventoryButtons buttons() {
                return new InventoryButtons(dir.resolve("buttons.json"),
                                new ButtonActivator(command -> { }, 1000));
        }

        private static ShareException awaitFailure(CompletableFuture<?> future) {
                ExecutionException e = assertThrows(ExecutionException.class, future::get);
                return assertInstanceOf(ShareException.class, e.getCause());
        }

        @Test
        void exportsUploadTheLiveState() throws Exception {
                FakeTransport transport = new FakeTransport();
                InventoryButtons buttons = buttons();
                buttons.applyImport(List.of(button("Home", "spawn")));
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons, new ZeaConfig(), Runnable::run);

                ShareCoordinator.Export result = coordinator.export(ShareType.INVENTORY_BUTTONS)
                                .get(5, java.util.concurrent.TimeUnit.SECONDS);

                assertEquals("key1", result.key().key());
                assertTrue(transport.uploads.get(0).contains("\"Home\""));
                assertTrue(transport.uploads.get(0).contains("\"format\": \"zea-share\""));
                assertEquals("", result.note());
        }

        @Test
        void exportsNoteUnfinishedButtons() throws Exception {
                FakeTransport transport = new FakeTransport();
                InventoryButtons buttons = buttons();
                buttons.upsert(button("Home", "spawn"));
                buttons.upsert(button("Draft", "")); // usable() is false — never exported
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons, new ZeaConfig(), Runnable::run);

                ShareCoordinator.Export result = coordinator.export(ShareType.INVENTORY_BUTTONS).get();

                assertEquals("1 unfinished button was left out", result.note());
                assertFalse(transport.uploads.get(0).contains("Draft"));
        }

        @Test
        void generalExportsCarryNoNote() throws Exception {
                ShareCoordinator coordinator = new ShareCoordinator(
                                new FakeTransport(), buttons(), new ZeaConfig(), Runnable::run);

                assertEquals("", coordinator.export(ShareType.GENERAL).get().note());
        }

        @Test
        void importsHoldUntilConfirmed() throws Exception {
                FakeTransport transport = new FakeTransport();
                InventoryButtons buttons = buttons();
                buttons.applyImport(List.of(button("Old", "old")));
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons, new ZeaConfig(), Runnable::run);
                ShareCoordinator.Export exported = coordinator.export(ShareType.INVENTORY_BUTTONS).get();
                String changed = transport.documents.get(exported.key().key()).replace("Old", "New");
                transport.documents.put(exported.key().key(), changed);

                ShareCoordinator.ImportPreview preview = coordinator.beginImport(exported.key().url()).get();

                assertTrue(preview.summary().contains("New"));
                assertTrue(preview.application().contains("Replaces your 1 button"));
                assertEquals("Old", buttons.buttons().get(0).name(),
                                "nothing applies before the player confirms");
        }

        @Test
        void confirmedImportsReplaceTheButtonSetAndPersist() throws Exception {
                FakeTransport transport = new FakeTransport();
                InventoryButtons buttons = buttons();
                buttons.applyImport(List.of(button("Old", "old")));
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons, new ZeaConfig(), Runnable::run);
                ShareCoordinator.Export exported = coordinator.export(ShareType.INVENTORY_BUTTONS).get();
                String changed = transport.documents.get(exported.key().key()).replace("Old", "New");
                transport.documents.put(exported.key().key(), changed);
                coordinator.beginImport("key1").get();

                ShareCoordinator.Applied applied = coordinator.confirmImport();

                assertEquals(ShareType.INVENTORY_BUTTONS, applied.type());
                assertFalse(coordinator.hasPendingImport());
                assertEquals("New", buttons.buttons().get(0).name());
                // A fresh facade reads the same set back from disk.
                assertEquals("New", buttons().buttons().get(0).name());
        }

        @Test
        void confirmingNothingIsPendingReturnsNull() {
                ShareCoordinator coordinator = new ShareCoordinator(
                                new FakeTransport(), buttons(), new ZeaConfig(), Runnable::run);

                assertNull(coordinator.confirmImport());
        }

        @Test
        void cancelledImportsApplyNothing() throws Exception {
                FakeTransport transport = new FakeTransport();
                InventoryButtons buttons = buttons();
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons, new ZeaConfig(), Runnable::run);
                coordinator.export(ShareType.INVENTORY_BUTTONS).get();
                coordinator.beginImport("key1").get();

                coordinator.cancelImport();
                assertNull(coordinator.confirmImport());
                assertEquals(0, buttons.buttons().size());
        }

        @Test
        void aNewImportReplacesThePendingOne() throws Exception {
                FakeTransport transport = new FakeTransport();
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons(), new ZeaConfig(), Runnable::run);
                coordinator.export(ShareType.INVENTORY_BUTTONS).get();
                coordinator.export(ShareType.GENERAL).get();

                coordinator.beginImport("key1").get();
                ShareCoordinator.ImportPreview second = coordinator.beginImport("key2").get();

                assertEquals(ShareType.GENERAL, second.type());
                assertEquals(ShareType.GENERAL, coordinator.confirmImport().type());
        }

        @Test
        void invalidKeysFailWithoutLeavingAPendingImport() {
                ShareCoordinator coordinator = new ShareCoordinator(
                                new FakeTransport(), buttons(), new ZeaConfig(), Runnable::run);

                ShareException e = awaitFailure(coordinator.beginImport("https://evil.example/x"));

                assertTrue(e.getMessage().contains("paste.helpcha.at"), e.getMessage());
                assertFalse(coordinator.hasPendingImport());
        }

        @Test
        void nonZeaDocumentsFailWithoutLeavingAPendingImport() {
                FakeTransport transport = new FakeTransport();
                transport.documents.put("abc", "{\"hello\":\"world\"}");
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons(), new ZeaConfig(), Runnable::run);

                awaitFailure(coordinator.beginImport("abc"));

                assertFalse(coordinator.hasPendingImport());
        }

        @Test
        void brokenPayloadsFailWithoutLeavingAPendingImport() {
                FakeTransport transport = new FakeTransport();
                transport.documents.put("abc", """
                                {"format":"zea-share","type":"inventory-buttons","version":1,
                                 "data":{"buttons":[{"name":"NoCommand"}]}}
                                """);
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons(), new ZeaConfig(), Runnable::run);

                awaitFailure(coordinator.beginImport("abc"));

                assertFalse(coordinator.hasPendingImport());
        }

        @Test
        void missingPastesFailWithAReadableReason() {
                ShareCoordinator coordinator = new ShareCoordinator(
                                new FakeTransport(), buttons(), new ZeaConfig(), Runnable::run);

                ShareException e = awaitFailure(coordinator.beginImport("nosuchkey"));

                assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
        }

        @Test
        void generalImportsApplyAndPersistTheConfiguration() throws Exception {
                FakeTransport transport = new FakeTransport();
                ZeaConfig config = new ZeaConfig();
                AtomicReference<ZeaConfig> saved = new AtomicReference<>();
                config.setSaveAction(() -> saved.set(config));
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons(), config, Runnable::run);
                coordinator.export(ShareType.GENERAL).get();
                config.chat.multiSendEnabled = true;

                coordinator.beginImport("key1").get();
                coordinator.confirmImport();

                // The export's value (false, the default) overwrote the change.
                assertEquals(false, config.chat.multiSendEnabled);
                assertEquals(config, saved.get());
        }

        @Test
        void futuresCompleteOnTheClientThreadExecutor() throws Exception {
                FakeTransport transport = new FakeTransport();
                AtomicReference<Thread> completionThread = new AtomicReference<>();
                CompletableFuture<Void> done = new CompletableFuture<>();
                ShareCoordinator coordinator = new ShareCoordinator(
                                transport, buttons(), new ZeaConfig(),
                                task -> {
                                        completionThread.set(Thread.currentThread());
                                        task.run();
                                        done.complete(null);
                                });

                coordinator.export(ShareType.GENERAL).get();

                assertTrue(done.isDone());
                assertEquals(Thread.currentThread(), completionThread.get(),
                                "completion must be marshalled through the client-thread executor");
        }
}
