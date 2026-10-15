package com.mentivo.dto.report;

import com.mentivo.dto.user.UserSummaryDto;
import com.mentivo.entity.enums.ReportCategory;
import com.mentivo.entity.enums.ReportStatus;
import java.time.LocalDateTime;

public class ReportDto {
    private Long id;
    private UserSummaryDto reporter;
    private UserSummaryDto reportedUser;
    private ReportCategory reasonCategory;
    private String description;
    private ReportStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;

    public ReportDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserSummaryDto getReporter() { return reporter; }
    public void setReporter(UserSummaryDto reporter) { this.reporter = reporter; }

    public UserSummaryDto getReportedUser() { return reportedUser; }
    public void setReportedUser(UserSummaryDto reportedUser) { this.reportedUser = reportedUser; }

    public ReportCategory getReasonCategory() { return reasonCategory; }
    public void setReasonCategory(ReportCategory reasonCategory) { this.reasonCategory = reasonCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
