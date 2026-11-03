package com.kali.profile;

import com.kali.identity.KaliUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    ProfileResponse getProfile(@AuthenticationPrincipal KaliUserPrincipal principal) {
        return profileService.getProfile(principal);
    }

    @PutMapping("/profile")
    ProfileResponse updateProfile(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profileService.updateProfile(principal, request);
    }
}
