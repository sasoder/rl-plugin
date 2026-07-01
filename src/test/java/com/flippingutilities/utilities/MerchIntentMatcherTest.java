package com.flippingutilities.utilities;

import com.flippingutilities.Utils;
import com.flippingutilities.model.OfferEvent;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.GrandExchangeOfferState;

public class MerchIntentMatcherTest {
    @Test
    public void tagsMatchingOfferAndConsumesOnlyMatchedLine() throws Exception {
        Path file = Files.createTempFile("merch-intents", ".jsonl");
        Files.write(file, (
                "{\"intentId\":\"miss\",\"itemId\":2,\"side\":\"buy\",\"qty\":3,\"price\":100,\"strategy\":\"patient-band\",\"note\":\"wrong item\"}\n" +
                "{\"intentId\":\"hit\",\"itemId\":1,\"side\":\"buy\",\"qty\":3,\"price\":100,\"strategy\":\"active-margin\",\"note\":\"matched\",\"hardExitAt\":\"2026-06-27T13:30:00Z\"}\n"
        ).getBytes(StandardCharsets.UTF_8));
        OfferEvent offer = Utils.offer(
                true,
                0,
                100,
                Instant.parse("2026-06-27T12:00:00Z"),
                0,
                GrandExchangeOfferState.BUYING,
                3
        );
        offer.setListedPrice(100);

        MerchIntentMatcher.tag(offer, file.toFile());

        assertEquals("hit", offer.getMerchIntentId());
        assertEquals("active-margin", offer.getMerchStrategy());
        assertEquals("matched", offer.getMerchNote());
        assertEquals("2026-06-27T13:30:00Z", offer.getMerchHardExitAt());
        String remaining = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertFalse(remaining.contains("\"hit\""));
        assertTrue(remaining.contains("\"miss\""));
    }
}
