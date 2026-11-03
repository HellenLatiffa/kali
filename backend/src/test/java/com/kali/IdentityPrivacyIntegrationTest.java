package com.kali;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kali.identity.KaliUserPrincipal;
import com.kali.identity.RegistrationRequest;
import com.kali.identity.UserAccountRepository;
import com.kali.privacy.HealthConsentService;
import com.kali.privacy.HealthDataConsentGuard;
import com.kali.privacy.HealthConsentRequiredException;
import com.kali.profile.UserProfileRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IdentityPrivacyIntegrationTest.SyntheticHealthRoute.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityPrivacyIntegrationTest {

    private static final String PASSWORD = "synthetic-test-passphrase-21";
    private static final AtomicInteger CLIENT_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessions;

    @Autowired
    private HealthConsentService consentService;

    @Autowired
    private HealthDataConsentGuard consentGuard;

    @BeforeEach
    void cleanAccounts() {
        users.deleteAll();
    }

    @Test
    void registrationLoginAndProfileUseAnHttpOnlySessionCookie() throws Exception {
        String email = uniqueEmail();
        Client client = newClient();

        mvc.perform(post("/api/v1/auth/register")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistrationRequest(email, PASSWORD, "Perfil sintético"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Perfil sintético"))
                .andReturn();

        String sessionHeader = login.getResponse().getHeader("Set-Cookie");
        assertThat(sessionHeader).contains("KALI_SESSION=").contains("HttpOnly").contains("SameSite=Lax");
        Cookie sessionCookie = login.getResponse().getCookie("KALI_SESSION");
        assertThat(sessionCookie).isNotNull();
        assertThat(client.csrfCookie().isHttpOnly()).isFalse();
        assertThat(login.getResponse().getHeader("Cache-Control")).contains("no-store");
        var storedSessions = sessions.findByPrincipalName(email);
        assertThat(storedSessions).hasSize(1);
        var securityContext = (org.springframework.security.core.context.SecurityContext)
                storedSessions.values().iterator().next().getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(((KaliUserPrincipal) securityContext.getAuthentication().getPrincipal()).getPassword())
                .isNull();

        mvc.perform(get("/api/v1/me")
                        .with(client.address())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void profileWritesAreScopedToTheAuthenticatedAccount() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        Client client = newClient();
        register(client, emailA, "Pessoa A");
        register(client, emailB, "Pessoa B");
        Cookie sessionA = login(client, emailA);
        UUID accountB = users.findByEmail(emailB).orElseThrow().getId();

        mvc.perform(put("/api/v1/me/profile")
                        .with(client.address())
                        .cookie(sessionA, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Pessoa A atualizada","userId":"%s"}
                                """.formatted(accountB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailA));

        assertThat(profiles.findById(accountB).orElseThrow().getDisplayName()).isEqualTo("Pessoa B");
        mvc.perform(get("/api/v1/me")
                        .with(client.address())
                        .cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailA))
                .andExpect(jsonPath("$.id").value(users.findByEmail(emailA).orElseThrow().getId().toString()));
    }

    @Test
    void explicitConsentIsVersionedAndRevocationBlocksHealthDataProcessing() throws Exception {
        String email = uniqueEmail();
        Client client = newClient();
        register(client, email, "Consentimento sintético");
        Cookie session = login(client, email);
        UUID userId = users.findByEmail(email).orElseThrow().getId();

        mvc.perform(get("/api/v1/privacy/consents/health-data")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRED"));

        assertThatThrownBy(() -> consentGuard.requirePermission(userId))
                .isInstanceOf(HealthConsentRequiredException.class);
        mvc.perform(get("/api/v1/cycles/test-sentinel")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        mvc.perform(put("/api/v1/privacy/consents/health-data")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepted":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRANTED"))
                .andExpect(jsonPath("$.history[0].purpose").value("HEALTH_DATA_PROCESSING"))
                .andExpect(jsonPath("$.history[0].policyVersion").value("2026-01"))
                .andExpect(jsonPath("$.history[0].noticeText").value(HealthConsentService.NOTICE))
                .andExpect(jsonPath("$.history[0].grantedAt").exists());
        consentGuard.requirePermission(userId);
        mvc.perform(get("/api/v1/cycles/test-sentinel")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/privacy/consents/health-data")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.history[0].revokedAt").exists());

        assertThatThrownBy(() -> consentGuard.requirePermission(userId))
                .isInstanceOf(HealthConsentRequiredException.class);
        var revokedHistory = consentService.getStatus(userId);
        assertThat(revokedHistory.status()).isEqualTo("REVOKED");
        Instant revokedAt = revokedHistory.history().getFirst().revokedAt();

        mvc.perform(put("/api/v1/privacy/consents/health-data")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepted":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GRANTED"))
                .andExpect(jsonPath("$.history.length()").value(2));
        consentGuard.requirePermission(userId);
        assertThat(consentService.getStatus(userId).history().getFirst().grantedAt())
                .isAfter(revokedAt);
    }

    @Test
    void exportAndDeletionRequirePasswordConfirmationAndDeletionInvalidatesSession() throws Exception {
        String email = uniqueEmail();
        Client client = newClient();
        register(client, email, "Exportação sintética");
        Cookie session = login(client, email);
        Client secondClient = newClient();
        Cookie secondSession = login(secondClient, email);

        mvc.perform(post("/api/v1/me/export")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"senha-incorreta-sintetica"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Não foi possível confirmar a solicitação."));

        mvc.perform(post("/api/v1/me/export")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value(email))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("password_hash")
                        .doesNotContain(PASSWORD));

        mvc.perform(delete("/api/v1/me")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("password", PASSWORD))))
                .andExpect(status().isNoContent());

        assertThat(users.findByEmail(email)).isEmpty();
        mvc.perform(get("/api/v1/me")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me")
                        .with(secondClient.address())
                        .cookie(secondSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationDoesNotRevealWhetherAnEmailExists() throws Exception {
        String email = uniqueEmail();
        Client client = newClient();
        register(client, email, "Pessoa sintética");
        MvcResult first = mvc.perform(post("/api/v1/auth/register")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistrationRequest(email.toUpperCase(), PASSWORD, "Outra pessoa"))))
                .andExpect(status().isAccepted())
                .andReturn();

        assertThat(objectMapper.readTree(first.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo("Solicitação processada. Se já houver uma conta com este email, tente entrar.");
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).isNotEqualTo(PASSWORD);
    }

    @Test
    void registrationRejectsInvalidInputWithoutPersistingAnAccount() throws Exception {
        Client client = newClient();

        mvc.perform(post("/api/v1/auth/register")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        assertThat(users.count()).isZero();
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesTheSession() throws Exception {
        String email = uniqueEmail();
        Client client = newClient();
        register(client, email, "Logout sintético");
        Cookie session = login(client, email);

        mvc.perform(post("/api/v1/auth/logout")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/logout")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me")
                        .with(client.address())
                        .cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRateLimitReturnsRetryAfterWithoutLeakingDetails() throws Exception {
        Client client = newClient();
        String email = uniqueEmail();
        for (int attempt = 0; attempt < 10; attempt++) {
            mvc.perform(post("/api/v1/auth/login")
                            .with(client.address())
                            .cookie(client.csrfCookie())
                            .header("X-XSRF-TOKEN", client.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(java.util.Map.of(
                                    "email", email,
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Email ou senha inválidos."));
        }

        mvc.perform(post("/api/v1/auth/login")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    private void register(Client client, String email, String displayName) throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistrationRequest(email, PASSWORD, displayName))))
                .andExpect(status().isAccepted());
    }

    private Cookie login(Client client, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("KALI_SESSION");
    }

    private Client newClient() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf")
                        .with(remoteAddress()))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new Client(cookie, cookie.getValue(), remoteAddress());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String uniqueEmail() {
        return "synthetic-" + UUID.randomUUID() + "@example.test";
    }

    private static RequestPostProcessor remoteAddress() {
        String address = "192.0.2." + CLIENT_SEQUENCE.getAndIncrement();
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private record Client(Cookie csrfCookie, String csrfToken, RequestPostProcessor address) {
    }

    @RestController
    @Profile("test")
    static class SyntheticHealthRoute {

        @GetMapping("/api/v1/cycles/test-sentinel")
        String healthRoute() {
            return "synthetic";
        }
    }
}
