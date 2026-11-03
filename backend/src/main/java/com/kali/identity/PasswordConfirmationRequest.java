package com.kali.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordConfirmationRequest(
        @NotBlank @Size(max = 72) String password
) {
}
