ALTER TABLE privacy_consents
    ADD COLUMN notice_text TEXT NOT NULL
        DEFAULT 'Consent notice text was not captured for this legacy record.';
