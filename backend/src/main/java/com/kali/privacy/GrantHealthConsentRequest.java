package com.kali.privacy;

import jakarta.validation.constraints.AssertTrue;

public record GrantHealthConsentRequest(boolean accepted) {

    @AssertTrue(message = "Consentimento explícito necessário.")
    public boolean isAccepted() {
        return accepted;
    }
}
