package com.mentivo.dto.report;

import com.mentivo.entity.enums.ReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReportRequest {

    @NotNull(message = "Reported user ID is required")
    private Long reportedUserId;

    @NotNull(message = "Report reason is required")
    private ReportCategory reasonCategory;

    @NotBlank(message = "Description of report is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    public CreateReportRequest() {}

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }

    public ReportCategory getReasonCategory() { return reasonCategory; }
    public void setReasonCategory(ReportCategory reasonCategory) { this.reasonCategory = reasonCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
