package com.kali.symptoms;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SymptomEntryRequest(
        @NotNull LocalDate entryDate,
        @NotNull SymptomType symptomType,
        @NotNull @Min(1) @Max(5) Integer intensity,
        @Size(max = 2000) String notes
) {
}
