package com.flippingutilities.utilities;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CurrentSlotExport {
    public final int slot;
    public final String state;
    public final String side;
    public final Integer itemId;
    public final Integer offerQty;
    public final Integer filledQty;
    public final Integer offerPrice;
    public final Integer filledPrice;
    public final String lastFillTime;
    public final String offerCreationTime;
    public final Long ageSeconds;
    public final String merchIntentId;
    public final String merchStrategy;
    public final String merchNote;
    public final String merchHardExitAt;
}
