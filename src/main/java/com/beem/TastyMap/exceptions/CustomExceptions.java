package com.beem.TastyMap.exceptions;

public class CustomExceptions {
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }
    public  static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
    public static class  NotFoundException extends RuntimeException {
        public  NotFoundException(String message) {
            super(message);
        }
    }
    public static class  AuthorizationException extends RuntimeException {
        public AuthorizationException(String message) {
            super(message);
        }
    }

    public static class TokenExpiredException extends RuntimeException {
        public  TokenExpiredException(String message) {
            super(message);
        }
    }
    public static class InvalidException extends RuntimeException {
        public InvalidException(String message) {
            super(message);
        }
    }
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
    public static class ForbiddenException extends RuntimeException {
        public  ForbiddenException(String message) {
            super(message);
        }
    }
    public static class BadRequestException extends RuntimeException {
        public  BadRequestException(String message) {
            super(message);
        }
    }
    public static class AlreadyVerifiedException extends RuntimeException{
        public AlreadyVerifiedException(String message){super(message);}
    }
    public static class RedisKeyExistsException extends RuntimeException{
        public RedisKeyExistsException(String message){
            super(message);
        }
    }
    public static class ServiceException extends RuntimeException {
        public  ServiceException(String message) {
            super(message);
        }
    }


}
