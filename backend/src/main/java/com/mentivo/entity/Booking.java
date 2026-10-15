package com.mentivo.entity;

import com.mentivo.entity.enums.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_ref", columnList = "booking_ref"),
        @Index(name = "idx_booking_mentor_date", columnList = "mentor_id, booking_date, start_time, end_time"),
        @Index(name = "idx_booking_learner", columnList = "learner_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_session", columnList = "session_id")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private User learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private MentorProfile mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private MentorSession mentorSession;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sessionPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mentorEarnings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 500)
    private String meetingUrl;

    @Column(columnDefinition = "TEXT")
    private String meetingNotes;

    @Column(length = 255)
    private String cancellationReason;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.bookingRef == null) {
            this.bookingRef = "MNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Booking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }

    public User getLearner() { return learner; }
    public void setLearner(User learner) { this.learner = learner; }

    public MentorProfile getMentor() { return mentor; }
    public void setMentor(MentorProfile mentor) { this.mentor = mentor; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

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

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    public MentorSession getMentorSession() { return mentorSession; }
    public void setMentorSession(MentorSession mentorSession) { this.mentorSession = mentorSession; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
