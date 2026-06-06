package com.beem.TastyMap;

import java.time.LocalDateTime;

public class BaseApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String errorCode;

    public static <T> BaseApiResponse<T> success(T data, String message){
        return baseSuccess(data, message);
    }
    public static <T> BaseApiResponse<T> success(T data){
        return baseSuccess(data, "Successfully getaway.");
    }

    public static <T> BaseApiResponse<T> error(String message, String errorCode){
        BaseApiResponse<T> errorResponse = new BaseApiResponse<>();
        errorResponse.success = false;
        errorResponse.message = message;
        errorResponse.timestamp = LocalDateTime.now();
        errorResponse.errorCode = errorCode;

        return errorResponse;
    }

    private static <T> BaseApiResponse<T> baseSuccess(T data, String message){
        BaseApiResponse<T> response = new BaseApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        response.timestamp = LocalDateTime.now();

        return response;
    }

    public BaseApiResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
