package com.beem.TastyMap.Exceptions;

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
    public static class ServiceException extends RuntimeException {
        public  ServiceException(String message) {
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

}
