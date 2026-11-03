package com.kali.privacy;

import java.util.List;

public record HealthConsentStatusResponse(
        String purpose,
        String policyVersion,
        String status,
        String notice,
        List<ConsentRecordResponse> history
) {
}
