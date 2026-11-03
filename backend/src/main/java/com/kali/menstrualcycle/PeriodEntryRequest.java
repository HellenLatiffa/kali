package com.kali.menstrualcycle;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PeriodEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull BleedingType bleedingType,
        @NotNull FlowLevel flowLevel,
        boolean cycleStart
) {

    @AssertTrue
    public boolean isCycleStartCompatibleWithBleedingType() {
        return !cycleStart || bleedingType == BleedingType.PERIOD;
    }
}
