package com.kali.privacy;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class HealthDataConsentWebConfiguration implements WebMvcConfigurer {

    private final HealthDataConsentInterceptor consentInterceptor;

    public HealthDataConsentWebConfiguration(HealthDataConsentInterceptor consentInterceptor) {
        this.consentInterceptor = consentInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(consentInterceptor)
                .addPathPatterns(
                        "/api/v1/periods/**",
                        "/api/v1/cycles/**",
                        "/api/v1/symptom-entries/**",
                        "/api/v1/mood-entries/**",
                        "/api/v1/diary/**",
                        "/api/v1/health/**",
                        "/api/v1/insights/**"
                );
    }
}
