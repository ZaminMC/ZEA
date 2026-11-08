package net.zaminmc.zea.buttons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryButtonStoreTest {
        @TempDir
        Path tempDir;

        private InventoryButtonStore newStore() {
                return new InventoryButtonStore(tempDir.resolve("buttons.json"));
        }

        private static InventoryButton usable(String name) {
                return InventoryButton.newButton(1).withName(name).withCommand("town spawn");
        }

        @Test
        void missingFileYieldsNoButtons() {
                assertTrue(newStore().load().isEmpty());
        }

        @Test
        void roundTripsButtons() {
                InventoryButtonStore store = newStore();
                List<InventoryButton> buttons = List.of(
                                usable("Home").withIcon("minecraft:compass"),
                                usable("Trade").withAnchor(ButtonAnchor.BOTTOM_LEFT).withOffset(-30, 4).withEnabled(false));

                store.save(buttons);
                List<InventoryButton> loaded = store.load();

                assertEquals(buttons, loaded);
        }

        @Test
        void corruptFileYieldsNoButtonsAndIsPreserved() throws IOException {
                Path file = tempDir.resolve("buttons.json");
                Files.writeString(file, "{ not json", UTF_8);

                assertTrue(newStore().load().isEmpty());
                assertEquals("{ not json", Files.readString(tempDir.resolve("buttons.json.corrupted"), UTF_8));
        }

        @Test
        void unusableEntriesAreDroppedOnLoad() throws IOException {
                Path file = tempDir.resolve("buttons.json");
                Files.writeString(file, """
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "name": "Good", "command": "town spawn", "icon": "minecraft:compass" },
                                    { "name": "No command" },
                                    { "name": "No icon", "command": "town spawn" }
                                  ]
                                }
                                """, UTF_8);

                List<InventoryButton> loaded = newStore().load();

                assertEquals(1, loaded.size());
                assertEquals("Good", loaded.get(0).name());
        }

        @Test
        void nullFieldsAndMissingVersionAreTolerated() throws IOException {
                Path file = tempDir.resolve("buttons.json");
                Files.writeString(file, """
                                {
                                  "buttons": [
                                    { "name": null, "command": "/g hi", "icon": "minecraft:paper", "anchor": null,
                                      "offsetX": null, "offsetY": null, "enabled": null }
                                  ]
                                }
                                """, UTF_8);

                List<InventoryButton> loaded = newStore().load();

                assertEquals(1, loaded.size());
                assertEquals(InventoryButton.DEFAULT_NAME, loaded.get(0).name());
                assertEquals("g hi", loaded.get(0).command());
                assertEquals(ButtonAnchor.TOP_RIGHT, loaded.get(0).anchor());
                assertEquals(InventoryButton.DEFAULT_OFFSET_X, loaded.get(0).offsetX());
                assertEquals(InventoryButton.DEFAULT_OFFSET_Y, loaded.get(0).offsetY());
                assertTrue(loaded.get(0).enabled());
        }

        @Test
        void duplicatedIdsAreReissuedOnLoad() throws IOException {
                UUID shared = UUID.randomUUID();
                Path file = tempDir.resolve("buttons.json");
                Files.writeString(file, """
                                {
                                  "version": 1,
                                  "buttons": [
                                    { "id": "%s", "name": "First", "command": "spawn", "icon": "minecraft:compass" },
                                    { "id": "%s", "name": "Second", "command": "spawn", "icon": "minecraft:paper" }
                                  ]
                                }
                                """.formatted(shared, shared), UTF_8);

                List<InventoryButton> loaded = newStore().load();

                assertEquals(2, loaded.size());
                assertEquals(2, loaded.stream().map(InventoryButton::id).distinct().count(),
                                "each button must keep a unique identity");
        }

        @Test
        void saveDropsUnusableButtons() {
                InventoryButtonStore store = newStore();
                store.save(List.of(usable("Good"), InventoryButton.newButton(2)));

                assertEquals(1, store.load().size());
        }

        @Test
        void saveWritesTheVersionedShape() throws IOException {
                newStore().save(List.of(usable("Home")));

                String json = Files.readString(tempDir.resolve("buttons.json"), UTF_8);
                assertTrue(json.contains("\"version\": 1"), "file should carry its schema version");
                assertTrue(json.contains("\"buttons\": ["), "file should carry the button list");
        }

        @Test
        void overwritingKeepsTheLastState() {
                InventoryButtonStore store = newStore();
                store.save(List.of(usable("First")));
                store.save(List.of(usable("Second"), usable("Third")));

                List<InventoryButton> loaded = store.load();

                assertEquals(2, loaded.size());
                assertEquals("Second", loaded.get(0).name());
        }
}
