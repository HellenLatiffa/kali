package com.kali.profile;

import com.kali.identity.KaliUserPrincipal;
import com.kali.identity.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private final UserAccountRepository users;
    private final UserProfileRepository profiles;

    public ProfileService(UserAccountRepository users, UserProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(KaliUserPrincipal principal) {
        var profile = profiles.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ProfileResponse(principal.id(), principal.email(), profile.getDisplayName());
    }

    @Transactional
    public ProfileResponse updateProfile(KaliUserPrincipal principal, UpdateProfileRequest request) {
        var account = users.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var profile = profiles.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profile.updateDisplayName(request.displayName().trim());
        return new ProfileResponse(account.getId(), account.getEmail(), profile.getDisplayName());
    }
}
