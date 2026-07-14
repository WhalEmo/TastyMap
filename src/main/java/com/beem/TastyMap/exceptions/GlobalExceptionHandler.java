package com.beem.TastyMap.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.Exception;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(CustomExceptions.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));

    }

    @ExceptionHandler(CustomExceptions.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(CustomExceptions.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthoExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.InvalidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleCredentialExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.AlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> alreadyExceptions(
            CustomExceptions.AlreadyVerifiedException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }
    @ExceptionHandler(CustomExceptions.TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> tokenExpired(
            CustomExceptions.TokenExpiredException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String combinedErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(combinedErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Beklenmedik bir sunucu hatası oluştu."+ ex));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "EMAIL_NOT_VERIFIED");
        body.put("message", ex.getMessage());
        body.put("email", ex.getEmail());

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

}

