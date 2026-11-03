package com.kali.privacy;

import com.kali.identity.KaliUserPrincipal;
import com.kali.identity.PasswordConfirmationRequest;
import com.kali.identity.UserAccountRepository;
import com.kali.menstrualcycle.PeriodEntryRepository;
import com.kali.menstrualcycle.PeriodEntryResponse;
import com.kali.profile.UserProfileRepository;
import com.kali.symptoms.SymptomEntryRepository;
import com.kali.symptoms.SymptomEntryResponse;
import com.kali.wellbeing.WellbeingEntryRepository;
import com.kali.wellbeing.WellbeingEntryResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Service
public class PersonalDataService {

    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final PrivacyConsentRepository consents;
    private final PeriodEntryRepository periodEntries;
    private final SymptomEntryRepository symptomEntries;
    private final WellbeingEntryRepository wellbeingEntries;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public PersonalDataService(
            UserAccountRepository users,
            UserProfileRepository profiles,
            PrivacyConsentRepository consents,
            PeriodEntryRepository periodEntries,
            SymptomEntryRepository symptomEntries,
            WellbeingEntryRepository wellbeingEntries,
            PasswordEncoder passwordEncoder,
            Clock clock,
            FindByIndexNameSessionRepository<? extends Session> sessions
    ) {
        this.users = users;
        this.profiles = profiles;
        this.consents = consents;
        this.periodEntries = periodEntries;
        this.symptomEntries = symptomEntries;
        this.wellbeingEntries = wellbeingEntries;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public ExportResponse export(KaliUserPrincipal principal, PasswordConfirmationRequest confirmation) {
        var user = users.findById(principal.id())
                .orElseThrow(PasswordConfirmationException::new);
        confirmPassword(user.getPasswordHash(), confirmation.password());
        var profile = profiles.findById(user.getId())
                .orElseThrow(PasswordConfirmationException::new);
        var consentRecords = consents
                .findAllByUserIdAndPurposeOrderByGrantedAtDescIdDesc(
                        user.getId(),
                        ConsentPurpose.HEALTH_DATA_PROCESSING
                )
                .stream()
                .map(ConsentRecordResponse::from)
                .toList();
        var periodRecords = periodEntries.findAllByUserIdOrderByEntryDateAsc(user.getId())
                .stream()
                .map(PeriodEntryResponse::from)
                .toList();
        var symptomRecords = symptomEntries.findAllByUserIdOrderByEntryDateAscCreatedAtAsc(user.getId())
                .stream()
                .map(SymptomEntryResponse::from)
                .toList();
        var wellbeingRecords = wellbeingEntries.findAllByUserIdOrderByEntryDateAscCreatedAtAsc(user.getId())
                .stream()
                .map(WellbeingEntryResponse::from)
                .toList();
        return new ExportResponse(
                clock.instant(),
                new ExportResponse.AccountData(
                        user.getId(),
                        user.getEmail(),
                        profile.getDisplayName(),
                        user.getCreatedAt()
                ),
                consentRecords,
                periodRecords,
                symptomRecords,
                wellbeingRecords
        );
    }

    @Transactional
    public void deleteAccount(KaliUserPrincipal principal, PasswordConfirmationRequest confirmation) {
        var user = users.findById(principal.id())
                .orElseThrow(PasswordConfirmationException::new);
        confirmPassword(user.getPasswordHash(), confirmation.password());
        sessions.findByPrincipalName(user.getEmail()).keySet().forEach(sessions::deleteById);
        users.delete(user);
    }

    private void confirmPassword(String passwordHash, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(password, passwordHash)) {
            throw new PasswordConfirmationException();
        }
    }
}
