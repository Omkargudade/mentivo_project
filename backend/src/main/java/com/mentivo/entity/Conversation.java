package com.mentivo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_pair", columnNames = {"user_one_id", "user_two_id"}),
        indexes = {
                @Index(name = "idx_conv_user_one", columnList = "user_one_id"),
                @Index(name = "idx_conv_user_two", columnList = "user_two_id"),
                @Index(name = "idx_conv_last_message", columnList = "last_message_at")
        }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_one_id", nullable = false)
    private User userOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_two_id", nullable = false)
    private User userTwo;

    @Column
    private LocalDateTime lastMessageAt;

    @Column(length = 500)
    private String lastMessagePreview;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.lastMessageAt == null) {
            this.lastMessageAt = LocalDateTime.now();
        }
    }

    public Conversation() {}

    public Conversation(User userOne, User userTwo) {
        this.userOne = userOne;
        this.userTwo = userTwo;
        this.lastMessageAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUserOne() { return userOne; }
    public void setUserOne(User userOne) { this.userOne = userOne; }

    public User getUserTwo() { return userTwo; }
    public void setUserTwo(User userTwo) { this.userTwo = userTwo; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
