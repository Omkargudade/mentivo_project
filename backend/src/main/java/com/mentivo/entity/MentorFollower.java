package com.mentivo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mentor_followers",
        uniqueConstraints = @UniqueConstraint(name = "uk_mentor_learner_follow", columnNames = {"mentor_id", "learner_id"}),
        indexes = {
                @Index(name = "idx_follower_mentor", columnList = "mentor_id"),
                @Index(name = "idx_follower_learner", columnList = "learner_id")
        }
)
public class MentorFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private User learner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public MentorFollower() {}

    public MentorFollower(MentorProfile mentor, User learner) {
        this.mentor = mentor;
        this.learner = learner;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MentorProfile getMentor() { return mentor; }
    public void setMentor(MentorProfile mentor) { this.mentor = mentor; }

    public User getLearner() { return learner; }
    public void setLearner(User learner) { this.learner = learner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
