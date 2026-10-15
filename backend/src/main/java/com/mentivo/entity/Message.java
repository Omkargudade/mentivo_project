package com.mentivo.entity;

import com.mentivo.entity.enums.MessageType;
import com.mentivo.entity.enums.ReadStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_msg_recipient_read", columnList = "recipient_id, read_status")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadStatus readStatus = ReadStatus.UNREAD;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Message() {}

    public Message(Conversation conversation, User sender, User recipient, String content, MessageType messageType) {
        this.conversation = conversation;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.messageType = (messageType != null) ? messageType : MessageType.TEXT;
        this.readStatus = ReadStatus.UNREAD;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public ReadStatus getReadStatus() { return readStatus; }
    public void setReadStatus(ReadStatus readStatus) { this.readStatus = readStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
