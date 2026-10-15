package com.mentivo.dto.call;

public class CallSessionCompleteResponse {
    private boolean eligibleForReview;
    private Long bookingId;
    private Long mentorId;
    private String mentorName;
    private Integer durationSeconds;
    private String message;

    public CallSessionCompleteResponse() {}

    public CallSessionCompleteResponse(boolean eligibleForReview, Long bookingId, Long mentorId, String mentorName, Integer durationSeconds, String message) {
        this.eligibleForReview = eligibleForReview;
        this.bookingId = bookingId;
        this.mentorId = mentorId;
        this.mentorName = mentorName;
        this.durationSeconds = durationSeconds;
        this.message = message;
    }

    public static CallSessionCompleteResponse notEligible(String message) {
        return new CallSessionCompleteResponse(false, null, null, null, 0, message);
    }

    public boolean isEligibleForReview() { return eligibleForReview; }
    public void setEligibleForReview(boolean eligibleForReview) { this.eligibleForReview = eligibleForReview; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
