package com.mentivo.entity;

import com.mentivo.entity.enums.VerificationLevel;
import com.mentivo.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_requests", indexes = {
        @Index(name = "idx_verif_status", columnList = "status"),
        @Index(name = "idx_verif_mentor", columnList = "mentor_id")
})
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationLevel levelRequested;

    @Column(length = 1000)
    private String documentUrls;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public VerificationRequest() {}

    public VerificationRequest(MentorProfile mentor, VerificationLevel levelRequested, String documentUrls, String notes) {
        this.mentor = mentor;
        this.levelRequested = levelRequested;
        this.documentUrls = documentUrls;
        this.notes = notes;
        this.status = VerificationStatus.PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MentorProfile getMentor() { return mentor; }
    public void setMentor(MentorProfile mentor) { this.mentor = mentor; }

    public VerificationLevel getLevelRequested() { return levelRequested; }
    public void setLevelRequested(VerificationLevel levelRequested) { this.levelRequested = levelRequested; }

    public String getDocumentUrls() { return documentUrls; }
    public void setDocumentUrls(String documentUrls) { this.documentUrls = documentUrls; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
