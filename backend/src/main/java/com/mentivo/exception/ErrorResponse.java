package com.mentivo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private String message;
    private String timestamp;
    private String path;
    private String errorCode;
    private Map<String, String> fieldErrors;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public ErrorResponse(String message, String errorCode, String path) {
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.path = path;
        this.timestamp = LocalDateTime.now().toString();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}
