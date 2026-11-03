package com.kali;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kali.identity.RegistrationRequest;
import com.kali.identity.UserAccountRepository;
import com.kali.menstrualcycle.PeriodEntryRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PeriodEntryIntegrationTest {

    private static final String PASSWORD = "synthetic-period-test-passphrase";
    private static final AtomicInteger CLIENT_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private PeriodEntryRepository periodEntries;

    @BeforeEach
    void cleanAccounts() {
        users.deleteAll();
    }

    @Test
    void periodEntriesRequireConsentArePrivateEditableExportedAndCascadeDeleted() throws Exception {
        Client clientA = newClient();
        Client clientB = newClient();
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        register(clientA, emailA);
        register(clientB, emailB);
        Cookie sessionA = login(clientA, emailA);
        Cookie sessionB = login(clientB, emailB);
        LocalDate entryDate = LocalDate.now().minusDays(3);

        mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(entryDate, "PERIOD", "MEDIUM")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        grantConsent(clientA, sessionA);
        grantConsent(clientB, sessionB);

        mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "entryDate", entryDate,
                                "bleedingType", "PERIOD"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        MvcResult created = mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(entryDate, "PERIOD", "MEDIUM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryDate").value(entryDate.toString()))
                .andExpect(jsonPath("$.bleedingType").value("PERIOD"))
                .andReturn();
        String entryId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mvc.perform(get("/api/v1/periods")
                        .with(clientB.address())
                        .cookie(sessionB)
                        .param("from", entryDate.toString())
                        .param("to", entryDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(put("/api/v1/periods/{id}", entryId)
                        .with(clientB.address())
                        .cookie(sessionB, clientB.csrfCookie())
                        .header("X-XSRF-TOKEN", clientB.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(entryDate, "SPOTTING", "LIGHT")))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/periods/{id}", entryId)
                        .with(clientB.address())
                        .cookie(sessionB, clientB.csrfCookie())
                        .header("X-XSRF-TOKEN", clientB.csrfToken()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(entryDate, "SPOTTING", "LIGHT")))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/v1/periods/{id}", entryId)
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(entryDate.minusDays(1), "SPOTTING", "LIGHT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryDate").value(entryDate.minusDays(1).toString()))
                .andExpect(jsonPath("$.flowLevel").value("LIGHT"));

        mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(LocalDate.now().plusDays(1), "PERIOD", "LIGHT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(post("/api/v1/me/export")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodEntries.length()").value(1))
                .andExpect(jsonPath("$.periodEntries[0].id").value(entryId));

        mvc.perform(delete("/api/v1/privacy/consents/health-data")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        mvc.perform(get("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA)
                        .param("from", entryDate.minusDays(1).toString())
                        .param("to", entryDate.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientA.address())
                        .cookie(sessionA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        users.delete(users.findByEmail(emailA).orElseThrow());
        assertThat(periodEntries.findById(UUID.fromString(entryId))).isEmpty();
    }

    @Test
    void periodQueriesRejectReversedAndExcessiveDateRanges() throws Exception {
        Client client = newClient();
        String email = uniqueEmail();
        register(client, email);
        Cookie session = login(client, email);
        grantConsent(client, session);

        mvc.perform(get("/api/v1/periods")
                        .with(client.address())
                        .cookie(session)
                        .param("from", "2026-01-02")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(get("/api/v1/periods")
                        .with(client.address())
                        .cookie(session)
                        .param("from", "2024-01-01")
                        .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void cycleEstimateRequiresFourExplicitStartsAndUsesOnlyTheAuthenticatedUsersHistory() throws Exception {
        Client clientA = newClient();
        Client clientB = newClient();
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        register(clientA, emailA);
        register(clientB, emailB);
        Cookie sessionA = login(clientA, emailA);
        Cookie sessionB = login(clientB, emailB);

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientA.address())
                        .cookie(sessionA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        grantConsent(clientA, sessionA);
        grantConsent(clientB, sessionB);

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientA.address())
                        .cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimateAvailable").value(false))
                .andExpect(jsonPath("$.recordedCycleStarts").value(0));

        LocalDate today = LocalDate.now();
        mvc.perform(post("/api/v1/periods")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(periodRequest(today.minusDays(1), "SPOTTING", "LIGHT", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        LocalDate[] starts = {
                today.minusDays(100),
                today.minusDays(71),
                today.minusDays(41),
                today.minusDays(10)
        };
        for (LocalDate start : starts) {
            mvc.perform(post("/api/v1/periods")
                            .with(clientA.address())
                            .cookie(sessionA, clientA.csrfCookie())
                            .header("X-XSRF-TOKEN", clientA.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(periodRequest(start, "PERIOD", "MEDIUM", true)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cycleStart").value(true));
        }

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientA.address())
                        .cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimateAvailable").value(true))
                .andExpect(jsonPath("$.recordedCycleStarts").value(4))
                .andExpect(jsonPath("$.intervalsUsed").value(3))
                .andExpect(jsonPath("$.medianCycleDays").value(30))
                .andExpect(jsonPath("$.shortestObservedCycleDays").value(29))
                .andExpect(jsonPath("$.longestObservedCycleDays").value(31))
                .andExpect(jsonPath("$.estimatedNextPeriod").value(today.plusDays(20).toString()))
                .andExpect(jsonPath("$.earliestPossibleDate").value(today.plusDays(19).toString()))
                .andExpect(jsonPath("$.latestPossibleDate").value(today.plusDays(21).toString()));

        for (long daysAgo : new long[]{220, 190, 160, 130}) {
            mvc.perform(post("/api/v1/periods")
                            .with(clientA.address())
                            .cookie(sessionA, clientA.csrfCookie())
                            .header("X-XSRF-TOKEN", clientA.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(periodRequest(today.minusDays(daysAgo), "PERIOD", "MEDIUM", true)))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientA.address())
                        .cookie(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordedCycleStarts").value(8))
                .andExpect(jsonPath("$.intervalsUsed").value(6))
                .andExpect(jsonPath("$.medianCycleDays").value(30));

        mvc.perform(post("/api/v1/me/export")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodEntries.length()").value(8))
                .andExpect(jsonPath("$.periodEntries[0].cycleStart").value(true));

        mvc.perform(get("/api/v1/insights/dashboard")
                        .with(clientB.address())
                        .cookie(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimateAvailable").value(false))
                .andExpect(jsonPath("$.recordedCycleStarts").value(0));
    }

    private void register(Client client, String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistrationRequest(email, PASSWORD, "Perfil sintético"))))
                .andExpect(status().isAccepted());
    }

    private Cookie login(Client client, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .with(client.address())
                        .cookie(client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("KALI_SESSION");
    }

    private void grantConsent(Client client, Cookie session) throws Exception {
        mvc.perform(put("/api/v1/privacy/consents/health-data")
                        .with(client.address())
                        .cookie(session, client.csrfCookie())
                        .header("X-XSRF-TOKEN", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accepted":true}
                                """))
                .andExpect(status().isOk());
    }

    private Client newClient() throws Exception {
        RequestPostProcessor address = remoteAddress();
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf").with(address))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new Client(cookie, cookie.getValue(), address);
    }

    private String periodRequest(LocalDate date, String type, String flow) throws Exception {
        return periodRequest(date, type, flow, false);
    }

    private String periodRequest(LocalDate date, String type, String flow, boolean cycleStart)
            throws Exception {
        return json(Map.of(
                "entryDate", date,
                "bleedingType", type,
                "flowLevel", flow,
                "cycleStart", cycleStart
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String uniqueEmail() {
        return "synthetic-period-" + UUID.randomUUID() + "@example.test";
    }

    private static RequestPostProcessor remoteAddress() {
        String address = "198.51.100." + CLIENT_SEQUENCE.getAndIncrement();
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private record Client(Cookie csrfCookie, String csrfToken, RequestPostProcessor address) {
    }
}
