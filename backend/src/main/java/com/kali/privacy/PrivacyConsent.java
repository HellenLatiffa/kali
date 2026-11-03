package com.kali.privacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "privacy_consents")
public class PrivacyConsent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private ConsentPurpose purpose;

    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;

    @Column(name = "notice_text", nullable = false, columnDefinition = "text")
    private String noticeText;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PrivacyConsent() {
    }

    public PrivacyConsent(
            UUID userId,
            ConsentPurpose purpose,
            String policyVersion,
            String noticeText,
            Instant grantedAt
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.policyVersion = policyVersion;
        this.noticeText = noticeText;
        this.grantedAt = grantedAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ConsentPurpose getPurpose() {
        return purpose;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public String getNoticeText() {
        return noticeText;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
