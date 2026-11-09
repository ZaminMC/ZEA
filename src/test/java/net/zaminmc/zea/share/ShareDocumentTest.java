package net.zaminmc.zea.share;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareDocumentTest {
        private static ShareDocument roundTrip(ShareDocument document) throws ShareException {
                return ShareDocument.parse(document.toJson());
        }

        @Test
        void documentsRoundTripThroughTheirSerializedForm() throws ShareException {
                JsonObject data = new JsonObject();
                data.addProperty("count", 3);
                ShareDocument document = ShareDocument.of(ShareType.INVENTORY_BUTTONS, data);

                ShareDocument parsed = roundTrip(document);

                assertEquals(ShareType.INVENTORY_BUTTONS, parsed.type());
                assertEquals(1, parsed.version());
                assertEquals(3, parsed.data().get("count").getAsInt());
        }

        @Test
        void serializationCarriesTheEnvelopeFieldsInOrder() {
                JsonObject data = new JsonObject();
                data.addProperty("x", 1);

                String json = ShareDocument.of(ShareType.GENERAL, data).toJson();

                assertTrue(json.contains("\"format\": \"zea-share\""), json);
                assertTrue(json.contains("\"type\": \"general\""), json);
                assertTrue(json.contains("\"version\": 1"), json);
                assertTrue(json.contains("\"data\""), json);
        }

        @Test
        void missingDataToleratedAsEmpty() throws ShareException {
                ShareDocument parsed = ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"general\",\"version\":1}");

                assertEquals(0, parsed.data().size());
        }

        @Test
        void unknownEnvelopeFieldsAreIgnored() throws ShareException {
                ShareDocument parsed = ShareDocument.parse("""
                                {"format":"zea-share","type":"general","version":1,
                                 "data":{},"futureField":{"nested":true}}
                                """);

                assertEquals(ShareType.GENERAL, parsed.type());
        }

        @Test
        void wrongFormatMarkerIsRejected() {
                ShareException e = assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"something-else\",\"type\":\"general\",\"version\":1,\"data\":{}}"));

                assertTrue(e.getMessage().contains("not a ZEA share"), e.getMessage());
        }

        @Test
        void missingFormatMarkerIsRejected() {
                assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"type\":\"general\",\"version\":1,\"data\":{}}"));
        }

        @Test
        void unknownTypeIsRejectedWithItsName() {
                ShareException e = assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"wardrobe\",\"version\":1,\"data\":{}}"));

                assertTrue(e.getMessage().contains("wardrobe"), e.getMessage());
        }

        @Test
        void futureVersionsAreRejectedNotGuessed() {
                ShareException e = assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"general\",\"version\":2,\"data\":{}}"));

                assertTrue(e.getMessage().contains("up to version 1"), e.getMessage());
        }

        @Test
        void nonPositiveVersionsAreRejected() {
                assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"general\",\"version\":0,\"data\":{}}"));
        }

        @Test
        void fractionalVersionsAreRejected() {
                assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"general\",\"version\":1.5,\"data\":{}}"));
        }

        @Test
        void invalidJsonIsRejected() {
                ShareException e = assertThrows(ShareException.class,
                                () -> ShareDocument.parse("{not json"));

                assertTrue(e.getMessage().contains("not valid JSON"), e.getMessage());
        }

        @Test
        void nonObjectDocumentsAreRejected() {
                ShareException e = assertThrows(ShareException.class,
                                () -> ShareDocument.parse("[\"zea-share\"]"));

                assertTrue(e.getMessage().contains("not a JSON object"), e.getMessage());
        }

        @Test
        void nonObjectDataIsRejected() {
                assertThrows(ShareException.class, () -> ShareDocument.parse(
                                "{\"format\":\"zea-share\",\"type\":\"general\",\"version\":1,\"data\":7}"));
        }

        @Test
        void shareTypeLookupAcceptsIdsAndPrefixes() throws ShareException {
                assertEquals(ShareType.INVENTORY_BUTTONS, ShareType.from("inven"));
                assertEquals(ShareType.INVENTORY_BUTTONS, ShareType.from("INVEN"));
                assertEquals(ShareType.INVENTORY_BUTTONS, ShareType.from("inventory-buttons"));
                assertEquals(ShareType.GENERAL, ShareType.from("gen"));
        }

        @Test
        void shareTypeLookupRejectsUnknownNames() {
                assertThrows(ShareException.class, () -> ShareType.from("buttons"));
                assertThrows(ShareException.class, () -> ShareType.from(null));
                assertThrows(ShareException.class, () -> ShareType.from(""));
        }
}
