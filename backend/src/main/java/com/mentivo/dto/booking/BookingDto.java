package com.mentivo.dto.booking;

import com.mentivo.dto.category.TopicDto;
import com.mentivo.dto.mentor.MentorCardDto;
import com.mentivo.dto.user.UserSummaryDto;
import com.mentivo.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingDto {
    private Long id;
    private String bookingRef;
    private UserSummaryDto learner;
    private MentorCardDto mentor;
    private TopicDto topic;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal sessionPrice;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    private BigDecimal mentorEarnings;
    private BookingStatus status;
    private String meetingUrl;
    private String meetingNotes;
    private String cancellationReason;
    private boolean hasReview;
    private String sessionTitle;
    private Long sessionId;
    private Long mentorUserId;
    private Long learnerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingDto() {}

    public Long getMentorUserId() { return mentorUserId; }
    public void setMentorUserId(Long mentorUserId) { this.mentorUserId = mentorUserId; }

    public Long getLearnerUserId() { return learnerUserId; }
    public void setLearnerUserId(Long learnerUserId) { this.learnerUserId = learnerUserId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public UserSummaryDto getLearner() { return learner; }
    public void setLearner(UserSummaryDto learner) { this.learner = learner; }

    public MentorCardDto getMentor() { return mentor; }
    public void setMentor(MentorCardDto mentor) { this.mentor = mentor; }

    public TopicDto getTopic() { return topic; }
    public void setTopic(TopicDto topic) { this.topic = topic; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getSessionPrice() { return sessionPrice; }
    public void setSessionPrice(BigDecimal sessionPrice) { this.sessionPrice = sessionPrice; }

    public BigDecimal getPlatformFee() { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee) { this.platformFee = platformFee; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getMentorEarnings() { return mentorEarnings; }
    public void setMentorEarnings(BigDecimal mentorEarnings) { this.mentorEarnings = mentorEarnings; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getMeetingNotes() { return meetingNotes; }
    public void setMeetingNotes(String meetingNotes) { this.meetingNotes = meetingNotes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public boolean isHasReview() { return hasReview; }
    public void setHasReview(boolean hasReview) { this.hasReview = hasReview; }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
