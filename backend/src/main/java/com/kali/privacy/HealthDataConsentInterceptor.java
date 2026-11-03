package com.kali.privacy;

import com.kali.identity.KaliUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class HealthDataConsentInterceptor implements HandlerInterceptor {

    private final HealthDataConsentGuard consentGuard;

    public HealthDataConsentInterceptor(HealthDataConsentGuard consentGuard) {
        this.consentGuard = consentGuard;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof KaliUserPrincipal user)) {
            throw new HealthConsentRequiredException();
        }
        consentGuard.requirePermission(user.id());
        return true;
    }
}
