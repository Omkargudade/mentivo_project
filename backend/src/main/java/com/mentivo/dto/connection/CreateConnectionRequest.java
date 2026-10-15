package com.mentivo.dto.connection;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateConnectionRequest {

    @NotNull(message = "Mentor or recipient user ID is required")
    private Long receiverId;

    @Size(max = 500, message = "Connection note cannot exceed 500 characters")
    private String message;

    public CreateConnectionRequest() {}

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
