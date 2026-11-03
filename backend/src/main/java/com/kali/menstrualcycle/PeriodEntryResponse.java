package com.kali.menstrualcycle;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodEntryResponse(
        UUID id,
        LocalDate entryDate,
        BleedingType bleedingType,
        FlowLevel flowLevel,
        boolean cycleStart
) {
    public static PeriodEntryResponse from(PeriodEntry entry) {
        return new PeriodEntryResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getBleedingType(),
                entry.getFlowLevel(),
                entry.isCycleStart()
        );
    }
}
