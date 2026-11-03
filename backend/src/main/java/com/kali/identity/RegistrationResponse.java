package com.kali.identity;

public record RegistrationResponse(String message) {

    public static RegistrationResponse accepted() {
        return new RegistrationResponse(
                "Solicitação processada. Se já houver uma conta com este email, tente entrar."
        );
    }
}
