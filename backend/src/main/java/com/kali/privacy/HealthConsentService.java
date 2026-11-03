package com.kali.privacy;

import com.kali.identity.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class HealthConsentService {

    private static final String LEGACY_NOTICE_TEXT =
            "Consent notice text was not captured for this legacy record.";

    public static final String NOTICE =
            "Autorizo a coleta e o processamento dos dados que eu registrar sobre ciclo menstrual, sintomas "
                    + "e bem-estar para oferecer os recursos de acompanhamento e histórico que eu solicitar. "
                    + "Nesta etapa, esses dados não são usados para publicidade nem enviados a provedores de IA. "
                    + "Posso revogar a qualquer momento; a revogação bloqueia novos processamentos de saúde, "
                    + "sem impedir o acesso ao perfil e aos meus direitos de exportação e exclusão.";

    private final PrivacyConsentRepository consents;
    private final UserAccountRepository users;
    private final Clock clock;
    private final String currentVersion;

    public HealthConsentService(
            PrivacyConsentRepository consents,
            UserAccountRepository users,
            Clock clock,
            @Value("${kali.privacy.health-consent-version}") String currentVersion
    ) {
        this.consents = consents;
        this.users = users;
        this.clock = clock;
        this.currentVersion = currentVersion;
    }

    @Transactional(readOnly = true)
    public HealthConsentStatusResponse getStatus(UUID userId) {
        List<PrivacyConsent> history = consents.findAllByUserIdAndPurposeOrderByGrantedAtDescIdDesc(
                userId,
                ConsentPurpose.HEALTH_DATA_PROCESSING
        );
        String status = status(history);
        return new HealthConsentStatusResponse(
                ConsentPurpose.HEALTH_DATA_PROCESSING.name(),
                currentVersion,
                status,
                NOTICE,
                history.stream().map(ConsentRecordResponse::from).toList()
        );
    }

    @Transactional
    public HealthConsentStatusResponse grant(UUID userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (consents.existsByUserIdAndPurposeAndPolicyVersionAndNoticeTextNot(
                userId,
                ConsentPurpose.HEALTH_DATA_PROCESSING,
                currentVersion,
                NOTICE
        )) {
            throw new ConsentVersionMismatchException();
        }

        List<PrivacyConsent> history = consents.findAllByUserIdAndPurposeOrderByGrantedAtDescIdDesc(
                userId,
                ConsentPurpose.HEALTH_DATA_PROCESSING
        );
        Instant latestEvent = history.stream()
                .flatMap(consent -> Stream.of(consent.getGrantedAt(), consent.getRevokedAt()))
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        Instant grantedAt = clock.instant();
        if (latestEvent != null && grantedAt.isBefore(latestEvent.plusMillis(1))) {
            grantedAt = latestEvent.plusMillis(1);
        }
        var active = consents.findFirstByUserIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDescIdDesc(
                userId,
                ConsentPurpose.HEALTH_DATA_PROCESSING
        );
        if (active.isPresent() && isCurrentConsent(active.get())) {
            return getStatus(userId);
        }

        if (active.isPresent()) {
            active.get().revoke(grantedAt);
            grantedAt = grantedAt.plusMillis(1);
        }
        consents.save(new PrivacyConsent(
                userId,
                ConsentPurpose.HEALTH_DATA_PROCESSING,
                currentVersion,
                NOTICE,
                grantedAt
        ));
        return getStatus(userId);
    }

    @Transactional
    public HealthConsentStatusResponse revoke(UUID userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        consents.findFirstByUserIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDescIdDesc(
                        userId,
                        ConsentPurpose.HEALTH_DATA_PROCESSING
                )
                .ifPresent(consent -> {
                    Instant revokedAt = clock.instant();
                    if (revokedAt.isBefore(consent.getGrantedAt().plusMillis(1))) {
                        revokedAt = consent.getGrantedAt().plusMillis(1);
                    }
                    consent.revoke(revokedAt);
                });
        return getStatus(userId);
    }

    @Transactional(readOnly = true)
    public void requireCurrentHealthConsent(UUID userId) {
        boolean granted = consents
                .findFirstByUserIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDescIdDesc(
                        userId,
                        ConsentPurpose.HEALTH_DATA_PROCESSING
                )
                .map(this::isCurrentConsent)
                .orElse(false);
        if (!granted) {
            throw new HealthConsentRequiredException();
        }
    }

    private String status(List<PrivacyConsent> history) {
        if (history.isEmpty()) {
            return "REQUIRED";
        }
        PrivacyConsent latest = history.getFirst();
        if (latest.getRevokedAt() != null) {
            return "REVOKED";
        }
        if (!isCurrentConsent(latest)) {
            return "RECONSENT_REQUIRED";
        }
        return "GRANTED";
    }

    private boolean isCurrentConsent(PrivacyConsent consent) {
        return consent.getPolicyVersion().equals(currentVersion)
                && !consent.getNoticeText().equals(LEGACY_NOTICE_TEXT)
                && consent.getNoticeText().equals(NOTICE);
    }
}
