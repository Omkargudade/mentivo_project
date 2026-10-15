package com.mentivo.entity;

import com.mentivo.entity.enums.ReportCategory;
import com.mentivo.entity.enums.ReportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_reported", columnList = "reported_user_id")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportCategory reasonCategory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Report() {}

    public Report(User reporter, User reportedUser, ReportCategory reasonCategory, String description) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reasonCategory = reasonCategory;
        this.description = description;
        this.status = ReportStatus.PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public User getReportedUser() { return reportedUser; }
    public void setReportedUser(User reportedUser) { this.reportedUser = reportedUser; }

    public ReportCategory getReasonCategory() { return reasonCategory; }
    public void setReasonCategory(ReportCategory reasonCategory) { this.reasonCategory = reasonCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
