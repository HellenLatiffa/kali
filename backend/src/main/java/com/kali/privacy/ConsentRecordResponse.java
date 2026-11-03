package com.kali.privacy;

import java.time.Instant;

public record ConsentRecordResponse(
        String purpose,
        String policyVersion,
        String noticeText,
        Instant grantedAt,
        Instant revokedAt
) {
    static ConsentRecordResponse from(PrivacyConsent consent) {
        return new ConsentRecordResponse(
                consent.getPurpose().name(),
                consent.getPolicyVersion(),
                consent.getNoticeText(),
                consent.getGrantedAt(),
                consent.getRevokedAt()
        );
    }
}
