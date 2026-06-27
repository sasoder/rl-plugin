package com.flippingutilities.utilities;

import com.flippingutilities.db.TradePersister;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
public class CurrentSlotsFileExporter {
    public static final File DEFAULT_PARENT_DIRECTORY = new File(TradePersister.PARENT_DIRECTORY, "current-slots");

    private final Gson gson;
    private final File parentDirectory;
    private AccountSlotsUpdate previouslyExportedSlotUpdate;

    public CurrentSlotsFileExporter(Gson gson) {
        this(gson, DEFAULT_PARENT_DIRECTORY);
    }

    public CurrentSlotsFileExporter(Gson gson, File parentDirectory) {
        this.gson = gson.newBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
        this.parentDirectory = parentDirectory;
    }

    public boolean export(AccountSlotsUpdate slotUpdate, boolean force) {
        if (slotUpdate == null || slotUpdate.rsn == null || slotUpdate.rsn.trim().isEmpty()) {
            return false;
        }
        if (!force && slotUpdate.equals(previouslyExportedSlotUpdate)) {
            return false;
        }

        try {
            write(slotUpdate);
            previouslyExportedSlotUpdate = slotUpdate;
            return true;
        } catch (IOException e) {
            log.warn("could not export current GE slots to file", e);
            return false;
        }
    }

    private void write(AccountSlotsUpdate slotUpdate) throws IOException {
        if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
            throw new IOException("unable to create current slots export directory");
        }

        Instant exportedAt = Instant.now();
        CurrentSlotsExport payload = new CurrentSlotsExport(
                slotUpdate.rsn,
                exportedAt.toString(),
                slotUpdate.slots.stream()
                        .map(slot -> toExportSlot(slot, exportedAt))
                        .collect(Collectors.toList())
        );

        File outFile = exportFile(slotUpdate.rsn);
        File tempFile = new File(parentDirectory, outFile.getName() + ".tmp");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(bufferedWriter)) {
            jsonWriter.setIndent("  ");
            gson.toJson(payload, CurrentSlotsExport.class, jsonWriter);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempFile.toPath());
            } catch (IOException ignored) {
            }
            throw e;
        }

        try {
            Files.move(tempFile.toPath(), outFile.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), outFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public File exportFile(String rsn) {
        return new File(parentDirectory, rsn + ".json");
    }

    public static File defaultExportFile(String rsn) {
        return new File(DEFAULT_PARENT_DIRECTORY, rsn + ".json");
    }

    private CurrentSlotExport toExportSlot(SlotState slot, Instant exportedAt) {
        String offerCreationTime = slot.getOfferCreationTime() == null
                ? null
                : slot.getOfferCreationTime().toInstant().toString();
        String lastFillTime = slot.getLastFilledTime() == null
                ? null
                : slot.getLastFilledTime().toInstant().toString();
        Long ageSeconds = slot.getOfferCreationTime() == null
                ? null
                : Math.max(0, exportedAt.getEpochSecond() - slot.getOfferCreationTime().toInstant().getEpochSecond());
        String side = slot.getIsBuyOffer() == null
                ? null
                : (slot.getIsBuyOffer() ? "buy" : "sell");

        return new CurrentSlotExport(
                slot.getIndex(),
                slot.getState() == null ? "EMPTY" : slot.getState(),
                side,
                slot.getItemId(),
                slot.getOfferQty(),
                slot.getFilledQty(),
                slot.getOfferPrice(),
                slot.getFilledPrice(),
                lastFillTime,
                offerCreationTime,
                ageSeconds,
                slot.getBeforeLogin() == null ? false : slot.getBeforeLogin(),
                slot.getMerchIntentId(),
                slot.getMerchStrategy(),
                slot.getMerchNote()
        );
    }

    public static String exportPathDescription() {
        return DEFAULT_PARENT_DIRECTORY.getPath() + File.separator + "<rsn>.json";
    }
}
