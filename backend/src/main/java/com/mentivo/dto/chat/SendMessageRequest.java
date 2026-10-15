package com.mentivo.dto.chat;

import com.mentivo.entity.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {

    private Long conversationId;

    @NotBlank(message = "Message content cannot be blank")
    private String content;

    private MessageType messageType = MessageType.TEXT;

    public SendMessageRequest() {}

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }
}
