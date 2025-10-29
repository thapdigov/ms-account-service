package az.kapital.msaccountservice.exception;

import az.kapital.msaccountservice.model.ErrorCode;
import az.kapital.msaccountservice.model.GlobalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<GlobalResponse> forbiddenAccessExistsExceptionHandler(ForbiddenAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GlobalResponse.builder()
                .id(UUID.randomUUID())
                .error_code(ErrorCode.INVALID_CREDENTIALS)
                .error_message(ex.getLocalizedMessage())
                .time(LocalDateTime.now())
                .build());

    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<GlobalResponse> alreadyExistsExceptionHandler(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(GlobalResponse.builder()
                .id(UUID.randomUUID())
                .error_code(ErrorCode.NOT_ALLOWED)
                .error_message(ex.getLocalizedMessage())
                .time(LocalDateTime.now())
                .build());

    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<GlobalResponse> invalidCredentialsExceptionHandler(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(GlobalResponse.builder()
                .id(UUID.randomUUID())
                .error_code(ErrorCode.NOT_FOUND)
                .error_message(ex.getLocalizedMessage())
                .time(LocalDateTime.now())
                .build());

    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalResponse> invalidTokenExceptionHandler(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(GlobalResponse.builder()
                .id(UUID.randomUUID())
                .error_code(ErrorCode.INVALID_TOKEN)
                .error_message(ex.getLocalizedMessage())
                .time(LocalDateTime.now())
                .build());

    }
}
