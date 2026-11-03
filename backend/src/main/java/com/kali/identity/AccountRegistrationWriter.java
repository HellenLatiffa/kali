package com.kali.identity;

import com.kali.profile.UserProfile;
import com.kali.profile.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AccountRegistrationWriter {

    private final UserAccountRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationWriter(
            UserAccountRepository users,
            UserProfileRepository profiles,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void create(RegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        int passwordBytes = request.password().getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes > 72) {
            throw new PasswordTooLongException();
        }
        if (users.existsByEmail(normalizedEmail)) {
            return;
        }

        UserAccount user = users.saveAndFlush(new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(request.password())
        ));
        profiles.save(new UserProfile(user.getId(), request.displayName().trim()));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
