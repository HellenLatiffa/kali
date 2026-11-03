package com.kali.identity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AccountRegistrationService {

    private final AccountRegistrationWriter writer;
    private final UserAccountRepository users;

    public AccountRegistrationService(AccountRegistrationWriter writer, UserAccountRepository users) {
        this.writer = writer;
        this.users = users;
    }

    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = AccountRegistrationWriter.normalizeEmail(request.email());
        try {
            writer.create(request);
        } catch (DataIntegrityViolationException exception) {
            if (!users.existsByEmail(normalizedEmail)) {
                throw exception;
            }
        }
        return RegistrationResponse.accepted();
    }
}
