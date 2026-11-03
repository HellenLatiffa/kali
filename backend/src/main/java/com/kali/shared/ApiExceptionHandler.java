package com.kali.shared;

import com.kali.privacy.HealthConsentRequiredException;
import com.kali.privacy.PasswordConfirmationException;
import com.kali.privacy.ConsentVersionMismatchException;
import com.kali.identity.PasswordTooLongException;
import com.kali.menstrualcycle.PeriodEntryConflictException;
import com.kali.shared.InvalidHealthEntryException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidRequest() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "Revise os dados informados."));
    }

    @ExceptionHandler(InvalidHealthEntryException.class)
    ResponseEntity<ApiError> invalidHealthEntry() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "Revise as datas informadas."));
    }

    @ExceptionHandler(PeriodEntryConflictException.class)
    ResponseEntity<ApiError> periodEntryConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DATA_CONFLICT", "Já existe um registro para esta data."));
    }

    @ExceptionHandler(PasswordTooLongException.class)
    ResponseEntity<ApiError> passwordTooLong() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "A senha excede o limite suportado."));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("INVALID_CREDENTIALS", "Email ou senha inválidos."));
    }

    @ExceptionHandler(HealthConsentRequiredException.class)
    ResponseEntity<ApiError> healthConsentRequired() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("HEALTH_CONSENT_REQUIRED",
                        "O consentimento vigente é necessário para processar dados de saúde."));
    }

    @ExceptionHandler(PasswordConfirmationException.class)
    ResponseEntity<ApiError> passwordConfirmationFailed() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("PASSWORD_CONFIRMATION_FAILED",
                        "Não foi possível confirmar a solicitação."));
    }

    @ExceptionHandler(ConsentVersionMismatchException.class)
    ResponseEntity<ApiError> consentVersionMismatch() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("CONSENT_VERSION_MISMATCH",
                        "A versão do aviso precisa ser atualizada antes de aceitar o novo texto."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> dataConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("DATA_CONFLICT", "Não foi possível concluir a solicitação."));
    }
}
