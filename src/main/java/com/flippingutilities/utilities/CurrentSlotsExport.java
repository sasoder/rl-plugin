package com.flippingutilities.utilities;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class CurrentSlotsExport {
    public final String rsn;
    public final String exportedAt;
    public final List<CurrentSlotExport> slots;
}
