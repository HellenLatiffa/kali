package com.kali.privacy;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HealthDataConsentGuard {

    private final HealthConsentService consents;

    public HealthDataConsentGuard(HealthConsentService consents) {
        this.consents = consents;
    }

    public void requirePermission(UUID authenticatedUserId) {
        consents.requireCurrentHealthConsent(authenticatedUserId);
    }
}
