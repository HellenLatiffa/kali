package com.kali.insights;

import com.kali.identity.KaliUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightsController {

    private final CycleEstimateService estimates;

    public InsightsController(CycleEstimateService estimates) {
        this.estimates = estimates;
    }

    @GetMapping("/dashboard")
    CycleEstimateResponse dashboard(@AuthenticationPrincipal KaliUserPrincipal principal) {
        return estimates.estimate(principal);
    }
}
