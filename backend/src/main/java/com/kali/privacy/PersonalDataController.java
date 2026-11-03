package com.kali.privacy;

import com.kali.identity.KaliUserPrincipal;
import com.kali.identity.PasswordConfirmationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/me")
public class PersonalDataController {

    private final PersonalDataService personalDataService;
    private final boolean secureCookie;

    public PersonalDataController(
            PersonalDataService personalDataService,
            @Value("${kali.security.cookie-secure:false}") boolean secureCookie
    ) {
        this.personalDataService = personalDataService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/export")
    ResponseEntity<ExportResponse> export(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody PasswordConfirmationRequest confirmation
    ) {
        ExportResponse export = personalDataService.export(principal, confirmation);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("kali-export-" + LocalDate.now() + ".json")
                                .build()
                                .toString())
                .body(export);
    }

    @DeleteMapping
    ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody PasswordConfirmationRequest confirmation,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        personalDataService.deleteAccount(principal, confirmation);
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            SecurityContextHolder.clearContext();
        }
        ResponseCookie expiredSession = ResponseCookie.from("KALI_SESSION", "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredSession.toString());
        return ResponseEntity.noContent().build();
    }
}
