package com.kali.profile;

import java.util.UUID;

public record ProfileResponse(UUID id, String email, String displayName) {
}
