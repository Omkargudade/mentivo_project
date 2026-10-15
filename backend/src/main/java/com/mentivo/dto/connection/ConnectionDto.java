package com.mentivo.dto.connection;

import com.mentivo.dto.user.UserSummaryDto;
import com.mentivo.entity.enums.ConnectionStatus;
import java.time.LocalDateTime;

public class ConnectionDto {
    private Long id;
    private UserSummaryDto sender;
    private UserSummaryDto receiver;
    private String message;
    private ConnectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConnectionDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSummaryDto getSender() { return sender; }
    public void setSender(UserSummaryDto sender) { this.sender = sender; }

    public UserSummaryDto getReceiver() { return receiver; }
    public void setReceiver(UserSummaryDto receiver) { this.receiver = receiver; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
