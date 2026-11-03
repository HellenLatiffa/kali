package com.kali.privacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, UUID> {

    Optional<PrivacyConsent> findFirstByUserIdAndPurposeAndRevokedAtIsNullOrderByGrantedAtDescIdDesc(
            UUID userId,
            ConsentPurpose purpose
    );

    boolean existsByUserIdAndPurposeAndPolicyVersionAndNoticeTextNot(
            UUID userId,
            ConsentPurpose purpose,
            String policyVersion,
            String noticeText
    );

    List<PrivacyConsent> findAllByUserIdAndPurposeOrderByGrantedAtDescIdDesc(
            UUID userId,
            ConsentPurpose purpose
    );
}
