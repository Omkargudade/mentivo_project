package com.mentivo.dto.admin;

import com.mentivo.entity.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public class VerificationReviewRequest {

    @NotNull(message = "Status is required (APPROVED or REJECTED)")
    private VerificationStatus status;

    private String reviewerNotes;

    public VerificationReviewRequest() {}

    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }

    public String getReviewerNotes() { return reviewerNotes; }
    public void setReviewerNotes(String reviewerNotes) { this.reviewerNotes = reviewerNotes; }
}
