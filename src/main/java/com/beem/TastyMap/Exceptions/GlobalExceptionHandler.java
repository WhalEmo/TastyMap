package com.beem.TastyMap.Exceptions;

import com.beem.TastyMap.BaseApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.Exception;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserExists(CustomExceptions.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(CustomExceptions.AuthenticationException.class)
    public ResponseEntity<String> handleAuthException(CustomExceptions.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(java.lang.Exception.class)
    public ResponseEntity<String> handleOtherExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bir hata oluştu: " + ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BaseApiResponse<Void>> handleIllegalStateException(IllegalStateException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        BaseApiResponse.error(ex.getMessage(),"ERR_SERVER")
                );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(CustomExceptions.RedisKeyExistsException.class)
    public ResponseEntity<BaseApiResponse<Void>> handleRedisKeyExistsException(CustomExceptions.RedisKeyExistsException ex){
        return ResponseEntity
                .status(HttpStatus.LOCKED)
                .body(
                        BaseApiResponse.error(ex.getMessage(), "ERR_SERVER")
                );
    }

}

