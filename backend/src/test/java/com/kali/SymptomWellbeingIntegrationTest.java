package com.kali;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kali.identity.RegistrationRequest;
import com.kali.identity.UserAccountRepository;
import com.kali.symptoms.SymptomEntryRepository;
import com.kali.wellbeing.WellbeingEntryRepository;
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
class SymptomWellbeingIntegrationTest {

    private static final String PASSWORD = "synthetic-wellbeing-test-passphrase";
    private static final AtomicInteger CLIENT_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private SymptomEntryRepository symptoms;

    @Autowired
    private WellbeingEntryRepository wellbeing;

    @BeforeEach
    void cleanAccounts() {
        users.deleteAll();
    }

    @Test
    void symptomsAndWellbeingArePrivateValidatedExportedAndDeletedWithAccount() throws Exception {
        Client clientA = newClient();
        Client clientB = newClient();
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();
        register(clientA, emailA);
        register(clientB, emailB);
        Cookie sessionA = login(clientA, emailA);
        Cookie sessionB = login(clientB, emailB);
        LocalDate date = LocalDate.now().minusDays(2);

        mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 3, "Synthetic note")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        grantConsent(clientA, sessionA);
        grantConsent(clientB, sessionB);

        MvcResult symptomOne = mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 3, "Synthetic note")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symptomType").value("CRAMPS"))
                .andReturn();
        String symptomId = responseId(symptomOne);

        MvcResult symptomTwo = mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 2, null)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondSymptomId = responseId(symptomTwo);

        mvc.perform(get("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/v1/symptom-entries")
                        .with(clientB.address())
                        .cookie(sessionB)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(put("/api/v1/symptom-entries/{id}", symptomId)
                        .with(clientB.address())
                        .cookie(sessionB, clientB.csrfCookie())
                        .header("X-XSRF-TOKEN", clientB.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 4, "Unauthorized update")))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 6, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(LocalDate.now().plusDays(1), 2, null)))
                .andExpect(status().isBadRequest());

        String oversizedNote = "x".repeat(2001);
        mvc.perform(post("/api/v1/symptom-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 2, oversizedNote)))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/symptom-entries/{id}", symptomId)
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(symptomRequest(date, 4, "Updated synthetic note")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intensity").value(4));

        MvcResult moodOne = mvc.perform(post("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date, "NEUTRAL", "Private synthetic reflection")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mood").value("NEUTRAL"))
                .andReturn();
        String moodId = responseId(moodOne);

        MvcResult moodTwo = mvc.perform(post("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date, "GOOD", null)))
                .andExpect(status().isCreated())
                .andReturn();
        String secondMoodId = responseId(moodTwo);

        mvc.perform(get("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/v1/mood-entries")
                        .with(clientB.address())
                        .cookie(sessionB)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date, "GOOD", null, 0)))
                .andExpect(status().isBadRequest());

        String oversizedReflection = "x".repeat(2001);
        mvc.perform(post("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date, "GOOD", oversizedReflection)))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/mood-entries/{id}", moodId)
                        .with(clientB.address())
                        .cookie(sessionB, clientB.csrfCookie())
                        .header("X-XSRF-TOKEN", clientB.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date, "VERY_GOOD", null)))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/v1/mood-entries/{id}", moodId)
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wellbeingRequest(date.minusDays(1), "GOOD", "Edited synthetic reflection")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryDate").value(date.minusDays(1).toString()));

        mvc.perform(delete("/api/v1/symptom-entries/{id}", secondSymptomId)
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/mood-entries/{id}", secondMoodId)
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken()))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/me/export")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptomEntries.length()").value(1))
                .andExpect(jsonPath("$.wellbeingEntries.length()").value(1))
                .andExpect(jsonPath("$.symptomEntries[0].notes").value("Updated synthetic note"))
                .andExpect(jsonPath("$.wellbeingEntries[0].reflection").value("Edited synthetic reflection"));

        mvc.perform(delete("/api/v1/privacy/consents/health-data")
                        .with(clientA.address())
                        .cookie(sessionA, clientA.csrfCookie())
                        .header("X-XSRF-TOKEN", clientA.csrfToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/mood-entries")
                        .with(clientA.address())
                        .cookie(sessionA)
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HEALTH_CONSENT_REQUIRED"));

        UUID userId = users.findByEmail(emailA).orElseThrow().getId();
        users.deleteById(userId);
        assertThat(symptoms.findById(UUID.fromString(symptomId))).isEmpty();
        assertThat(wellbeing.findById(UUID.fromString(moodId))).isEmpty();
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

    private String symptomRequest(LocalDate date, int intensity, String notes) throws Exception {
        return json(Map.of(
                "entryDate", date,
                "symptomType", "CRAMPS",
                "intensity", intensity,
                "notes", notes == null ? "" : notes
        ));
    }

    private String wellbeingRequest(LocalDate date, String mood, String reflection) throws Exception {
        return wellbeingRequest(date, mood, reflection, 4);
    }

    private String wellbeingRequest(LocalDate date, String mood, String reflection, int stress) throws Exception {
        return json(Map.of(
                "entryDate", date,
                "mood", mood,
                "stressLevel", stress,
                "energyLevel", 4,
                "sleepQuality", 3,
                "reflection", reflection == null ? "" : reflection
        ));
    }

    private String responseId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String uniqueEmail() {
        return "synthetic-wellbeing-" + UUID.randomUUID() + "@example.test";
    }

    private static RequestPostProcessor remoteAddress() {
        String address = "203.0.113." + CLIENT_SEQUENCE.getAndIncrement();
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private record Client(Cookie csrfCookie, String csrfToken, RequestPostProcessor address) {
    }
}
