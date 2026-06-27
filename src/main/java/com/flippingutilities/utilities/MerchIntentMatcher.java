package com.flippingutilities.utilities;

import com.flippingutilities.db.TradePersister;
import com.flippingutilities.model.OfferEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MerchIntentMatcher {
    public static final File DEFAULT_PARENT_DIRECTORY = new File(TradePersister.PARENT_DIRECTORY, "merch-intents");

    public static void tag(OfferEvent offer, String rsn) {
        if (offer == null || rsn == null || rsn.trim().isEmpty()) {
            return;
        }
        File file = new File(DEFAULT_PARENT_DIRECTORY, rsn + ".jsonl");
        tag(offer, file);
    }

    static void tag(OfferEvent offer, File file) {
        if (offer == null || file == null) {
            return;
        }
        if (!file.exists()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            List<String> keep = new ArrayList<>();
            JsonObject matched = null;
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                JsonObject intent = new JsonParser().parse(line).getAsJsonObject();
                if (matched == null && matches(offer, intent)) {
                    matched = intent;
                } else {
                    keep.add(line);
                }
            }
            if (matched == null) {
                return;
            }
            offer.setMerchIntentId(text(matched, "intentId"));
            offer.setMerchStrategy(text(matched, "strategy"));
            offer.setMerchNote(text(matched, "note"));
            Files.write(file.toPath(), keep, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            log.warn("could not match merch intent", e);
        }
    }

    private static boolean matches(OfferEvent offer, JsonObject intent) {
        return offer.getItemId() == number(intent, "itemId")
                && offer.isBuy() == "buy".equals(text(intent, "side"))
                && offer.getTotalQuantityInTrade() == number(intent, "qty")
                && offer.getListedPrice() == number(intent, "price");
    }

    private static int number(JsonObject intent, String key) {
        return intent.has(key) && !intent.get(key).isJsonNull() ? intent.get(key).getAsInt() : 0;
    }

    private static String text(JsonObject intent, String key) {
        return intent.has(key) && !intent.get(key).isJsonNull() ? intent.get(key).getAsString() : null;
    }
}
