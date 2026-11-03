package com.kali.identity;

import com.kali.profile.UserProfile;
import com.kali.profile.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationWriterTest {

    private static final String PASSWORD = "synthetic-unit-test-passphrase";

    @Mock
    private UserAccountRepository users;

    @Mock
    private UserProfileRepository profiles;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountRegistrationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new AccountRegistrationWriter(users, profiles, passwordEncoder);
    }

    @Test
    void normalizesEmailAndStoresOnlyAnEncodedPassword() {
        when(users.existsByEmail("person@example.test")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$12$synthetic-hash");
        when(users.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.<UserAccount>getArgument(0));

        writer.create(new RegistrationRequest("Person@Example.test", PASSWORD, "  Pessoa sintética  "));

        ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(account.capture());
        assertThat(account.getValue().getId()).isNotNull();
        assertThat(account.getValue().getEmail()).isEqualTo("person@example.test");
        assertThat(account.getValue().getPasswordHash()).isEqualTo("$2a$12$synthetic-hash");
        assertThat(account.getValue().getPasswordHash()).doesNotContain(PASSWORD);

        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        verify(profiles).save(profile.capture());
        assertThat(profile.getValue().getUserId()).isNotNull();
        assertThat(profile.getValue().getDisplayName()).isEqualTo("Pessoa sintética");
    }

    @Test
    void duplicateEmailDoesNotHashOrPersistAnotherAccount() {
        when(users.existsByEmail("person@example.test")).thenReturn(true);

        writer.create(new RegistrationRequest("Person@Example.test", PASSWORD, "Outra pessoa"));

        verify(users, never()).saveAndFlush(any(UserAccount.class));
        verifyNoInteractions(profiles, passwordEncoder);
    }

    @Test
    void rejectsPasswordsThatExceedBcryptByteLimit() {
        String multibytePassword = "á".repeat(37);

        assertThatThrownBy(() -> writer.create(new RegistrationRequest(
                "person@example.test",
                multibytePassword,
                "Pessoa sintética"
        ))).isInstanceOf(PasswordTooLongException.class);

        verify(users, never()).saveAndFlush(any(UserAccount.class));
        verifyNoInteractions(profiles, passwordEncoder);
    }
}
