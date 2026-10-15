package com.mentivo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_block_pair", columnNames = {"blocker_id", "blocked_id"}),
        indexes = {
                @Index(name = "idx_block_blocker", columnList = "blocker_id"),
                @Index(name = "idx_block_blocked", columnList = "blocked_id")
        }
)
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Block() {}

    public Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBlocker() { return blocker; }
    public void setBlocker(User blocker) { this.blocker = blocker; }

    public User getBlocked() { return blocked; }
    public void setBlocked(User blocked) { this.blocked = blocked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
