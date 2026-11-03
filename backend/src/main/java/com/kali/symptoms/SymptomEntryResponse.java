package com.kali.symptoms;

import java.time.LocalDate;
import java.util.UUID;

public record SymptomEntryResponse(
        UUID id,
        LocalDate entryDate,
        SymptomType symptomType,
        int intensity,
        String notes
) {
    public static SymptomEntryResponse from(SymptomEntry entry) {
        return new SymptomEntryResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getSymptomType(),
                entry.getIntensity(),
                entry.getNotes()
        );
    }
}
