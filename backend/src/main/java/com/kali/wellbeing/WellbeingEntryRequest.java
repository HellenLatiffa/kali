package com.kali.wellbeing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WellbeingEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull MoodLevel mood,
        @Min(1) @Max(5) Integer stressLevel,
        @Min(1) @Max(5) Integer energyLevel,
        @Min(1) @Max(5) Integer sleepQuality,
        @Size(max = 2000) String reflection
) {
}
