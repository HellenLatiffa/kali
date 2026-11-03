package com.kali.privacy;

import com.kali.identity.KaliUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacy/consents/health-data")
public class PrivacyController {

    private final HealthConsentService consentService;

    public PrivacyController(HealthConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping
    HealthConsentStatusResponse getStatus(@AuthenticationPrincipal KaliUserPrincipal principal) {
        return consentService.getStatus(principal.id());
    }

    @PutMapping
    HealthConsentStatusResponse grant(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody GrantHealthConsentRequest request
    ) {
        return consentService.grant(principal.id());
    }

    @DeleteMapping
    HealthConsentStatusResponse revoke(@AuthenticationPrincipal KaliUserPrincipal principal) {
        return consentService.revoke(principal.id());
    }
}
