package com.mentivo.dto.chat;

import com.mentivo.entity.enums.MessageType;
import com.mentivo.entity.enums.ReadStatus;
import java.time.LocalDateTime;

public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String content;
    private MessageType messageType;
    private ReadStatus readStatus;
    private LocalDateTime createdAt;

    public MessageDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public ReadStatus getReadStatus() { return readStatus; }
    public void setReadStatus(ReadStatus readStatus) { this.readStatus = readStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
