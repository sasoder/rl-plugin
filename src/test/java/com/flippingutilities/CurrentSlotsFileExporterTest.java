package com.flippingutilities;

import com.flippingutilities.utilities.AccountSlotsUpdate;
import com.flippingutilities.utilities.CurrentSlotsFileExporter;
import com.flippingutilities.utilities.SlotState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CurrentSlotsFileExporterTest {
    @Test
    public void exportsActiveOfferLimitPriceAndEmptySlots() throws Exception {
        Path tempDir = Files.createTempDirectory("fu-current-slots");
        CurrentSlotsFileExporter exporter = new CurrentSlotsFileExporter(new Gson(), tempDir.toFile());

        SlotState activeSell = new SlotState(
                false,
                32032,
                0,
                null,
                0,
                SlotState.convertStateEnum(GrandExchangeOfferState.SELLING),
                261,
                41324,
                0,
                Date.from(Instant.parse("2026-06-23T10:00:00Z")),
                false,
                "intent-1",
                "active-margin",
                "test note"
        );
        SlotState empty = SlotState.createEmptySlot(1);

        assertTrue(exporter.export(new AccountSlotsUpdate("Evidence", Arrays.asList(activeSell, empty)), true));

        String json = new String(Files.readAllBytes(tempDir.resolve("Evidence.json")), StandardCharsets.UTF_8);
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();

        assertEquals("Evidence", root.get("rsn").getAsString());
        assertEquals(2, root.getAsJsonArray("slots").size());

        JsonObject slot0 = root.getAsJsonArray("slots").get(0).getAsJsonObject();
        assertEquals(0, slot0.get("slot").getAsInt());
        assertEquals("ACTIVE", slot0.get("state").getAsString());
        assertEquals("sell", slot0.get("side").getAsString());
        assertEquals(32032, slot0.get("itemId").getAsInt());
        assertEquals(261, slot0.get("offerQty").getAsInt());
        assertEquals(0, slot0.get("filledQty").getAsInt());
        assertEquals(41324, slot0.get("offerPrice").getAsInt());
        assertEquals("2026-06-23T10:00:00Z", slot0.get("offerCreationTime").getAsString());
        assertEquals("intent-1", slot0.get("merchIntentId").getAsString());
        assertEquals("active-margin", slot0.get("merchStrategy").getAsString());
        assertEquals("test note", slot0.get("merchNote").getAsString());

        JsonObject slot1 = root.getAsJsonArray("slots").get(1).getAsJsonObject();
        assertEquals(1, slot1.get("slot").getAsInt());
        assertEquals("EMPTY", slot1.get("state").getAsString());
        assertTrue(slot1.get("side").isJsonNull());
        assertTrue(slot1.get("itemId").isJsonNull());
    }
}
