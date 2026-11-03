package com.kali.wellbeing;

import java.time.LocalDate;
import java.util.UUID;

public record WellbeingEntryResponse(
        UUID id,
        LocalDate entryDate,
        MoodLevel mood,
        Integer stressLevel,
        Integer energyLevel,
        Integer sleepQuality,
        String reflection
) {
    public static WellbeingEntryResponse from(WellbeingEntry entry) {
        return new WellbeingEntryResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getMood(),
                entry.getStressLevel(),
                entry.getEnergyLevel(),
                entry.getSleepQuality(),
                entry.getReflection()
        );
    }
}
