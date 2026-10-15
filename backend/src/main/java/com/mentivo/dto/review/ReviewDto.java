package com.mentivo.dto.review;

import com.mentivo.dto.user.UserSummaryDto;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReviewDto {
    private Long id;
    private Long bookingId;
    private Long mentorId;
    private UserSummaryDto learner;
    private Integer rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private String sessionTitle;
    private LocalDate sessionDate;
    private boolean verified;

    public ReviewDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public UserSummaryDto getLearner() { return learner; }
    public void setLearner(UserSummaryDto learner) { this.learner = learner; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
