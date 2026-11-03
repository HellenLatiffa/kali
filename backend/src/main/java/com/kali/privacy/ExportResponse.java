package com.kali.privacy;

import com.kali.menstrualcycle.PeriodEntryResponse;
import com.kali.symptoms.SymptomEntryResponse;
import com.kali.wellbeing.WellbeingEntryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExportResponse(
        Instant exportedAt,
        AccountData account,
        List<ConsentRecordResponse> consents,
        List<PeriodEntryResponse> periodEntries,
        List<SymptomEntryResponse> symptomEntries,
        List<WellbeingEntryResponse> wellbeingEntries
) {
    public record AccountData(UUID id, String email, String displayName, Instant createdAt) {
    }
}
